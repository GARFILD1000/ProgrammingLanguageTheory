package com.example.automate.view

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.*
import android.util.Log
import android.view.*
import java.util.*
import android.view.MotionEvent
import com.example.automate.utils.OrthogonalJPS
import android.view.ScaleGestureDetector
import androidx.core.content.ContextCompat
import com.example.automate.R
import com.example.automate.model.*
import com.example.automate.utils.TreeVertex
import kotlinx.coroutines.*
import kotlin.collections.HashMap
import kotlin.math.*


class GraphView : FrameLayout {
    private var chain: Chain = Chain()

    private var graphViews: HashMap<String, GraphElementView> = HashMap()

    private var connections: LinkedList<LineLayout> = LinkedList()
    private var createdLine: LineLayout = LineLayout()
    private var touchPosition: PointF = PointF(0f, 0f)
    private var editorAreaSize: Point = Point(4000, 4000)

    val editorViewSize = Point()
    private val pathfinder = OrthogonalJPS(editorAreaSize.x, editorAreaSize.y, 20)
    private val pathfinderObstacles = LinkedList<Rect>()

    private var lastPointerCount = 0
    private var scaleFactor = 1f
    private var maxScale = 2f
    private var minScale = 0.1f
    private var scaling = false
    private var scaleCenter = PointF().apply { x = 0f; y = 0f }
    private var dragDeltaX = 0f
    private var dragDeltaY = 0f
    private var difference = PointF()
    private var pinchListener = ScaleGestureDetector(context, EditorScaleListener())

    private var treeStructure: TreeVertex<GraphElementView>? = null
    private val freeFragments = LinkedList<GraphElementView>()

    private val lastTouchPoint = PointF(0f, 0f)
    private val lineWidth = 2f
    private val densityDpi =
        context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    private val hardJob = SupervisorJob()
    private val editorScope = CoroutineScope(Dispatchers.Main + hardJob)

    var selectedFragment: String = ""

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initView(context)
    }

    inner class EditorScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleEditor(detector)
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            scaling = true
            goodScaleFactor = 1.0
            startDistance = -1.0
            startScaleFactor = scaleFactor
            scaleCenter.x = 1f
            scaleCenter.x = 1f
            detector?.let {
                scaleCenter.x = it.focusX
                scaleCenter.y = it.focusY
            }
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            scaling = false
            scaleConnectionsWidth()
            invalidate()
            super.onScaleEnd(detector)
        }
    }


    private fun autoPosition(onEnded: () -> Unit) {
        chain.graph ?: return
        treeStructure = buildTree(graphViews, chain.graph!!.data.id)
        if (treeStructure == null) {
            return
        }
        freeFragments.clear()
        pathfinderObstacles.clear()
        for (view in graphViews) {
            if (!treeStructure!!.inTree(view.value)) {
                freeFragments.add(view.value)
            }
        }
        treeStructure!!.data.measure(0, 0)
        val fragmentWidth = treeStructure!!.data.measuredWidth
        val fragmentHeight = treeStructure!!.data.measuredHeight
        val cellWidth = treeStructure!!.data.measuredWidth * 1.5f
        val cellHeight = treeStructure!!.data.measuredHeight * 1.5f
        val paddingLeft = 100f
        val paddingTop = 50f

        val cellCounts = treeStructure!!.getEveryWidth()
        var maxWidth = 0
        for (cellCount in cellCounts) {
            maxWidth = max(maxWidth, cellCount)
        }
        maxWidth *= cellWidth.toInt()
        val animationSet = AnimatorSet()
        val listOfAnimations = LinkedList<ObjectAnimator>()
        //move fragments according to their position in tree structure
        for (i in cellCounts.indices) {
            val newY = paddingTop + i * cellHeight
            val fragments = treeStructure!!.getAllAtDepth(i)
            val widthStep = maxWidth / fragments.size
//            fragments.first?.apply {
//            }
            for (j in fragments.indices) {
                pathfinderObstacles.addLast(Rect())
                fragments[j].data.layerRow = fragments.size - j - 1
                val newX = paddingLeft + widthStep * j + widthStep / 2
                fragments[j].data.pathfinderPositionX = newX.roundToInt()
                fragments[j].data.pathfinderPositionY = newY.roundToInt()
                pathfinderObstacles.last.left = newX.roundToInt()
                pathfinderObstacles.last.top = newY.roundToInt()
                pathfinderObstacles.last.right = (newX + fragmentWidth).roundToInt()
                pathfinderObstacles.last.bottom = (newY + fragmentHeight).roundToInt()
                listOfAnimations.add(
                    ObjectAnimator.ofFloat(fragments[j].data, "x", newX)
                )
                listOfAnimations.add(
                    ObjectAnimator.ofFloat(fragments[j].data, "y", newY)
                )
            }
        }
        //move fragments, that are not connected to others
        if (freeFragments.isNotEmpty()) {
            pathfinderObstacles.addLast(Rect())
            pathfinderObstacles.last.left = -1
        }
        for (i in freeFragments.indices) {
            freeFragments[i].layerColumn = -1
            freeFragments[i].layerRow = i
            val newX = paddingLeft + cellCounts.size * cellWidth
            val newY = paddingTop + cellHeight * i
            freeFragments[i].pathfinderPositionX = newX.roundToInt()
            freeFragments[i].pathfinderPositionY = newY.roundToInt()
            if (pathfinderObstacles.last.left > 0) {
                pathfinderObstacles.last.left =
                    min(pathfinderObstacles.last.left, newX.roundToInt())
                pathfinderObstacles.last.top =
                    min(pathfinderObstacles.last.top, newY.roundToInt())
                pathfinderObstacles.last.right =
                    max(pathfinderObstacles.last.right, (newX + fragmentWidth).roundToInt())
                pathfinderObstacles.last.bottom =
                    max(pathfinderObstacles.last.bottom, (newY + fragmentHeight).roundToInt())
            } else {
                pathfinderObstacles.last.left = newX.roundToInt()
                pathfinderObstacles.last.top = newY.roundToInt()
                pathfinderObstacles.last.right = (newX + fragmentWidth).roundToInt()
                pathfinderObstacles.last.bottom = (newY + fragmentHeight).roundToInt()
            }
            listOfAnimations.add(
                ObjectAnimator.ofFloat(freeFragments[i], "x", newX)
            )
            listOfAnimations.add(
                ObjectAnimator.ofFloat(freeFragments[i], "y", newY)
            )
        }
        animationSet.playTogether(listOfAnimations as Collection<Animator>)
        animationSet.setDuration(300)
        animationSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                onEnded()
            }

            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
            override fun onAnimationRepeat(p0: Animator?) {}
        })
        animationSet.start()
        editorScope.launch {
            delay(100)
            recalculateConnections()
            invalidate()
        }.invokeOnCompletion {
        }
    }

    private fun buildTree(
        fragments: HashMap<String, GraphElementView>,
        startElementId: String
    ): TreeVertex<GraphElementView>? {
        val startFragment = fragments.get(startElementId)
        //var treeStructure: TreeVertex<GraphElementView>? = null
        if (startFragment != null) {
            treeStructure = TreeVertex(fragments.get(startElementId)!!)
            buildSubTree(fragments, treeStructure!!, chain.graph!!)
        }
        return treeStructure
    }

    private fun buildSubTree(
        fragments: HashMap<String, GraphElementView>,
        parentVertex: TreeVertex<GraphElementView>,
        dataTree: TreeVertex<GraphElement>,
        currentDepth: Int = 0
    ) {
        for (child in dataTree.childs) {
            val fragment = fragments.get(child.data.id)
            fragment ?: continue
//            if (fragment != null && (fragment.layerColumn < 0 || fragment.layerColumn == currentDepth)
//                && !treeStructure!!.inTree(fragment)
//            ) {
            val newVertex = parentVertex.addChild(fragment)
            fragment.layerColumn = currentDepth
            buildSubTree(fragments, newVertex, child, currentDepth + 1)
//            }
        }
    }

    private var startDistance = -1.0
    private var goodScaleFactor = 1.0
    private var startScaleFactor = 1f
    private var scaleFilterCounter = 0

    private var scaleDeltaAccumulator = 0L

    private fun calculateScaleFactor(event: MotionEvent) {
        if (event.pointerCount > 1) {
            val pointerOneX = event.getX(0).toDouble()
            val pointerOneY = event.getY(0).toDouble()
            val pointerTwoX = event.getX(1).toDouble()
            val pointerTwoY = event.getY(1).toDouble()
            val distance = sqrt(
                ((pointerTwoX - pointerOneX) * (pointerTwoX - pointerOneX))
                        + ((pointerTwoY - pointerOneY) * (pointerTwoY - pointerOneY))
            ) * scaleFactor
            if (startDistance < 0) {
                startDistance = distance
            } else {
                goodScaleFactor = distance / startDistance
            }
        }
    }

    //scaling editor view with touchscreen noise filter
    private fun scaleEditor(detector: ScaleGestureDetector) {
        if (editorAreaSize.x * scaleFactor > editorViewSize.x &&
            editorAreaSize.y * scaleFactor > editorViewSize.y
            || detector.scaleFactor > 1f
        ) {
            val previousScaleFactor = scaleFactor
            scaleDeltaAccumulator += detector.timeDelta

            if (scaleFilterCounter % 2 == 0) {
                //scaleFactor *= detector.scaleFactor
                scaleFactor = (startScaleFactor.toDouble() * goodScaleFactor).toFloat()
                if (scaleFactor > maxScale) {
                    scaleFactor = maxScale
                } else if (scaleFactor < minScale) {
                    scaleFactor = minScale
                }
                //Log.d("Area size","${editorAreaSize.x  * scaleFactor} ${editorAreaSize.y  * scaleFactor}")
                //Log.d("View size","${editorViewSize.x} ${editorViewSize.y}")
                scaleCenter.x = detector.focusX
                scaleCenter.y = detector.focusY

                var diffX = scaleCenter.x
                var diffY = scaleCenter.y
                val prevDiffSizeX = difference.x
                val prevDiffSizeY = difference.y
                calcViewSizeDiff()
                diffX =
                    (difference.x + diffX * scaleFactor) - (prevDiffSizeX + diffX * previousScaleFactor)
                diffY =
                    (difference.y + diffY * scaleFactor) - (prevDiffSizeY + diffY * previousScaleFactor)

                x += -diffX
                y += -diffY
                scaleX = scaleFactor
                scaleY = scaleFactor

                scaleDeltaAccumulator = 0
            }
            scaleFilterCounter++
        }
    }

    //smooth scaling editor
    fun scaleEditor(
        newScaleFactor: Float,
        scrollX: Float,
        scrollY: Float,
        millis: Long,
        onEnd: () -> Unit
    ) {
        val prevX = x
        val prevY = y
        animate()
            .scaleX(newScaleFactor)
            .scaleY(newScaleFactor)
            .x(prevX + scrollX)
            .y(prevY + scrollY)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationEnd(p0: Animator?) {
                    onEnd()
                }

                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {}
            })
            .setDuration(millis)
            .start()
    }


    private fun outOfBounds(margin: Float = 0f): Boolean {
        return x - difference.x + editorAreaSize.x + margin < editorViewSize.x
                || x + difference.x - margin > 0
                || y - difference.y + editorAreaSize.y + margin < editorViewSize.y
                || y + difference.y - margin > 0
    }

    private fun outOfBoundsX(margin: Float = 0f): Boolean {
        return x - difference.x + editorAreaSize.x + margin < editorViewSize.x
                || x + difference.x - margin > 0
    }

    private fun outOfBoundsY(margin: Float = 0f): Boolean {
        return y - difference.y + editorAreaSize.y + margin < editorViewSize.y
                || y + difference.y - margin > 0
    }

    //fit editor area bounds in editor view
    private fun fitBounds(timeForFit: Long) {
        //calcViewSizeDiff()
        var newX = x
        var newY = y
        if (x - difference.x + editorAreaSize.x < editorViewSize.x) {
            newX = +difference.x - editorAreaSize.x + editorViewSize.x
        }
        if (x + difference.x > 0) {
            newX = -difference.x
        }
        if (y - difference.y + editorAreaSize.y < editorViewSize.y) {
            newY = +difference.y - editorAreaSize.y + editorViewSize.y
        }
        if (y + difference.y > 0) {
            newY = -difference.y
        }
        animate().x(newX).y(newY).setDuration(timeForFit).start()
    }

    //calculate diff between editor view normal size and scaled
    private fun calcViewSizeDiff() {
        difference.x = (editorAreaSize.x.toFloat() - editorAreaSize.x.toFloat() * scaleFactor) / 2f
        difference.y = (editorAreaSize.y.toFloat() - editorAreaSize.y.toFloat() * scaleFactor) / 2f
    }

    private fun initView(context: Context) {
        setWillNotDraw(false)
        val display =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getSize(editorViewSize)
        setOnTouchListener { _, event ->
            when (event!!.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    if (!scaling) smoothDragAnimation()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaling) {
                        dragEditor(event)
                    } else {
                        calculateScaleFactor(event)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    cinematicAnimation.cancel()
                    lastTouchPoint.x = event.rawX
                    lastTouchPoint.y = event.rawY
                }
            }
            pinchListener.onTouchEvent(event)
            true
        }
    }

    private var cinematicAnimation = TimeAnimator()

    private fun smoothDragAnimation() {
        val startSpeedX = dragDeltaX / 15f
        val startSpeedY = dragDeltaY / 15f
        val directionX = if (dragDeltaX < 0) -1f else 1f
        val directionY = if (dragDeltaY < 0) -1f else 1f
        val accelerationX = startSpeedX / 400f * -1f
        val accelerationY = startSpeedY / 400f * -1f
        val minSpeed = 0.0001
        cinematicAnimation = TimeAnimator().apply {
            setTimeListener { timeAnimator, totalTime, deltaTime ->
                val speedX = startSpeedX + accelerationX * totalTime / 2f
                val speedY = startSpeedY + accelerationY * totalTime / 2f
                if (!outOfBounds(100f) &&
                    (speedX * directionX > minSpeed || speedY * directionY > minSpeed)
                ) {
                    x += speedX * deltaTime
                    y += speedY * deltaTime
                } else {
                    timeAnimator.cancel()
                    fitBounds(300)
                }
            }
        }
        cinematicAnimation.start()
    }

    private fun dragEditor(event: MotionEvent) {
        val pointX = event.rawX
        val pointY = event.rawY
        if (lastPointerCount == event.pointerCount) {
            dragDeltaX = pointX - lastTouchPoint.x
            dragDeltaY = pointY - lastTouchPoint.y
            val newX = x + dragDeltaX
            val newY = y + dragDeltaY
            x = newX
            y = newY
        }
        lastTouchPoint.x = pointX
        lastTouchPoint.y = pointY
        lastPointerCount = event.pointerCount
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    private val rand = Random().apply { setSeed(System.currentTimeMillis()) }

    private fun addGraphView(graphElement: GraphElement, parentElement: GraphElementView) {
        val newPosX = parentElement.x
        val newPosY = parentElement.y

        val element = newElement(graphElement).apply {
            x = newPosX
            y = newPosY
        }
        addView(element)
        graphViews.put(element.data.id, element)
    }

    private var ghostFragment: ImageView? = null

    //creating new video view
    private fun newElement(graphElement: GraphElement): GraphElementView {
        val elementView = GraphElementView(context, graphElement)

        elementView.onCreateFragment = {
            val newElement = GraphElement().apply {
                id = UUID.randomUUID().toString()
            }
            addGraphView(newElement, it)
            connections.add(LineLayout().apply {
                fromElementId = it.data.id
                toElementId = newElement.id
                fromConnectType = LineLayout.FORWARD_CONNECTOR
                toConnectType = LineLayout.BACKWARD_CONNECTOR
            })
            autoPosition {
                //                Log.d("GraphView","Positioned!")
//                recalculateConnections()
//                invalidate()
            }
        }

        elementView.onCreateConnection = { fromElement, fromConnectionType ->
            createdLine = LineLayout().apply {
                fromElementId = fromElement.id
                fromConnectType = fromConnectionType
            }
            if (ghostFragment == null) {
                elementView.changeStrokeColor(true)
                ghostFragment = ImageView(context)
                ghostFragment!!.setImageBitmap(elementView.getBitmap())
                addView(ghostFragment)
                elementView.shadow(true)
            }
        }

        elementView.onDragConnection = {
            if (ghostFragment != null) {
                ghostFragment!!.x = it.x
                ghostFragment!!.y = it.y
                touchPosition = it
            }
        }

        elementView.onDragConnectionFinished = { point ->
            if (ghostFragment != null) {
                val pointToCheck = PointF(
                    ghostFragment!!.x + ghostFragment!!.measuredWidth / 2,
                    ghostFragment!!.y + ghostFragment!!.measuredHeight / 2
                )
                removeView(ghostFragment)
                ghostFragment = null
                touchPosition = point
                //saveConnection(pointToCheck)
                autoPosition { }
                elementView.changeStrokeColor(false)
                elementView.shadow(false)
            }
        }

        elementView.onClicked = { elementId ->
            selectFragment(elementId, true)
        }
//
//        elementView.onTitleChanged = { assetId, title ->
//            changeChoicesText(assetId, title)
//            updateProjectData()
//        }
//
//        elementView.onCreatePopup = {
//        }
//
////        videoView.onSelect = {
////            videoFragments.get(it)?.changeStrokeColor(true)
////        }
//
//        elementView.onFragmentCreated = {
//            editorScope.launch {
//                //recalculateConnections()
//            }.invokeOnCompletion {
//                invalidate()
//                updateProjectData()
//            }
//        }
//        elementView.onPlayAsset = {
//            onPlayAsset(it)
//        }
//        elementView.onAddVideo = {
//            onAddVideoToAsset(it)
//        }
        return elementView
    }

    fun Point.toPointF(): PointF {
        return PointF(this.x.toFloat(), this.y.toFloat())
    }

    fun PointF.toPoint(): Point {
        return Point(this.x.roundToInt(), this.y.roundToInt())
    }

    private fun globalCoordinatesToLocal(globalPoint: PointF): PointF {
        val localPoint = PointF(0f, 0f)
        //there we get location of editor view
        val locationOnScreen = IntArray(2)
        getLocationOnScreen(locationOnScreen)
        //and there we add scaled position of global point
        localPoint.x += (globalPoint.x - locationOnScreen[0]) / scaleFactor
        localPoint.y += (globalPoint.y - locationOnScreen[1]) / scaleFactor
        return localPoint
    }

    private fun localCoordinatesToGlobal(localPoint: PointF): PointF {
        val globalPoint = PointF(0f, 0f)
        //there we get location of editor view
        val locationOnScreen = IntArray(2)
        getLocationOnScreen(locationOnScreen)
        //and there we add scaled position of global point
        globalPoint.x = locationOnScreen[0] + (localPoint.x) * scaleFactor
        globalPoint.y = locationOnScreen[1] + (localPoint.y) * scaleFactor
        return globalPoint
    }

//    private fun saveConnection(position: PointF) {
//        if (createdLine.hasStart()) {
//            var toFragment: GraphElementView? = null
//            val fromFragment: GraphElementView? = graphViews.get(createdLine.fromElementId)
//            val toConnectType = if (createdLine.fromConnectType == LineLayout.FORWARD_CONNECTOR) {
//                LineLayout.BACKWARD_CONNECTOR
//            } else {
//                LineLayout.FORWARD_CONNECTOR
//            }
//            for (fragment in graphViews) {
//                val videoView = fragment.value
//                if (videoView.contains(position)) {
//                    toFragment = videoView
//                }
//            }
//            if (toFragment != null && fromFragment != null && toFragment != fromFragment) {
//                val newChoice = Choice().apply {
//                    assetId = fromFragment.asset.assetId
//                    choiceId = toFragment.asset.assetId
//                    text = toFragment.title
//                }
//                if (!fromFragment.asset.hasChoice(newChoice)) {
//                    fromFragment.asset.choices.add(newChoice)
//                    val newLine = LineLayout()
//                    newLine.fromAssetId = createdLine.fromAssetId
//                    newLine.fromConnectType = createdLine.fromConnectType
//                    newLine.toAssetId = toFragment.asset.assetId
//                    newLine.toConnectType = toConnectType
//                    connections.add(newLine)
//                }
//            }
//        }
//        createdLine.clear()
//    }

//    private fun recalculateCreatedConnection() {
//        if (createdLine.hasStart()) {
//            pathfinder.clearObstacles()
//            for (fragment in videoFragments) {
//                val margin = 0
//                val size = fragment.value.getSize(margin)
//                val position = fragment.value.getPosition(margin)
//                pathfinder.addObstacle(
//                    position.x,
//                    position.y,
//                    position.x + size.x,
//                    position.y + size.y
//                )
//            }
//            val fromFragment: GraphElementView? = videoFragments.get(createdLine.fromAssetId)
//            var endPositionMargin = Point().apply {
//                x = touchPosition.x.toInt()
//                y = touchPosition.y.toInt()
//            }
//            var endPosition: Point? = null
//            for (fragment in videoFragments) {
//                if (fragment.value.contains(touchPosition.x, touchPosition.y)) {
//                    endPosition =
//                        fragment.value.getConnectorPosition(LineLayout.BACKWARD_CONNECTOR, 0)
//                    endPositionMargin =
//                        fragment.value.getConnectorPosition(LineLayout.BACKWARD_CONNECTOR, 30)
//                }
//            }
//            val startPositionMargin =
//                fromFragment!!.getConnectorPosition(createdLine.fromConnectType, 30)
//            var tempPath = pathfinder.findPath(
//                startPositionMargin.x, startPositionMargin.y,
//                endPositionMargin.x, endPositionMargin.y
//            )
//            if (!tempPath.isEmpty()) {
//                val startPosition = fromFragment.getConnectorPosition(createdLine.fromConnectType)
//                if (endPosition != null) {
//                    tempPath = pathfinder.makePretty(
//                        tempPath, OrthogonalJPS.PathPoint(startPosition.x, startPosition.y),
//                        OrthogonalJPS.PathPoint(endPosition.x, endPosition.y)
//                    )
//                } else {
//                    tempPath = pathfinder.makePretty(
//                        tempPath, OrthogonalJPS.PathPoint(startPosition.x, startPosition.y),
//                        OrthogonalJPS.PathPoint(endPositionMargin.x, endPositionMargin.y)
//                    )
//                }
//                createdLine.path = tempPath
//            }
//            pathfinder.clearBoard()
//        }
//    }

    private fun recalculateConnections() {
        pathfinder.clearObstacles()
//        for (fragment in videoFragments){
//            val margin = 0
//            val size = fragment.value.getSize(margin)
//            val position = fragment.value.getPosition(margin)
//            pathfinder.addObstacle(position.x, position.y, position.x + size.x, position.y + size.y)
//        }
        for (obstacle in pathfinderObstacles) {
            pathfinder.addObstacle(obstacle.left, obstacle.top, obstacle.right, obstacle.bottom)
        }
        Log.d("Calc connection", "Start recalculating")
        for (line in connections) {
            Log.d("Calc connection", "${line.fromConnectType} ${line.toConnectType}")
            val fromFragment: GraphElementView? = graphViews.get(line.fromElementId)
            val toFragment: GraphElementView? = graphViews.get(line.toElementId)
            if (fromFragment != null && toFragment != null) {
                var fromFragmentPosition =
                    Point(fromFragment.pathfinderPositionX, fromFragment.pathfinderPositionY)
                var toFragmentPosition =
                    Point(toFragment.pathfinderPositionX, toFragment.pathfinderPositionY)
                val startPositionMargin = fromFragment.getConnectorPosition(
                    fromFragmentPosition, line.fromConnectType,
                    (pathfinder.mTileSize.x * 2f).roundToInt()
                )
                val stopPositionMargin = toFragment.getConnectorPosition(
                    toFragmentPosition, line.toConnectType,
                    (pathfinder.mTileSize.x * 2f).roundToInt()
                )
                var tempPath = pathfinder.findPath(
                    startPositionMargin.x, startPositionMargin.y,
                    stopPositionMargin.x, stopPositionMargin.y
                )
                Log.d("Calc connection", "Path: ")
                tempPath.forEach {
                    Log.d("Calc connection", "${it.x} ${it.y}")
                }
                if (!tempPath.isEmpty()) {
                    val startPosition =
                        fromFragment.getConnectorPosition(
                            fromFragmentPosition,
                            line.fromConnectType
                        )
                    val stopPosition =
                        toFragment.getConnectorPosition(toFragmentPosition, line.toConnectType)
                    tempPath = pathfinder.makePretty(
                        tempPath,
                        OrthogonalJPS.PathPoint(startPosition.x, startPosition.y),
                        OrthogonalJPS.PathPoint(stopPosition.x, stopPosition.y),
                        pathfinder.mTileSize.x*2// + fromFragment.layerRow * 5
                    )
                    line.path = tempPath
                } else {
                    line.path = LinkedList<OrthogonalJPS.PathPoint>().apply{
                        add(OrthogonalJPS.PathPoint(startPositionMargin.x, startPositionMargin.y))
                        add(OrthogonalJPS.PathPoint(stopPositionMargin.x, stopPositionMargin.y))
                    }
                    Log.d("Editor", "Empty Path")
                }
            }
            pathfinder.clearBoard()
        }
    }
//
//    private fun recalculateConnectionsInRow() {
//        pathfinder.clearObstacles()
//        for (fragment in videoFragments) {
//            val margin = 0
//            val size = fragment.value.getSize(margin)
//            val position = fragment.value.getPosition(margin)
//            pathfinder.addObstacle(position.x, position.y, position.x + size.x, position.y + size.y)
//        }
//        for (line in connections) {
//            val fromFragment: GraphElementView? = videoFragments.get(line.fromAssetId)
//            val toFragment: GraphElementView? = videoFragments.get(line.toAssetId)
//            if (fromFragment != null && toFragment != null) {
//                var fragmentPosition =
//                    Point(fromFragment.pathfinderPositionX, fromFragment.pathfinderPositionY)
//
//                val startPositionMargin = fromFragment.getConnectorPosition(
//                    fragmentPosition, line.fromConnectType,
//                    60
//                )
//                val stopPositionMargin = toFragment.getConnectorPosition(
//                    fragmentPosition, line.toConnectType,
//                    60
//                )
//                var tempPath = pathfinder.findPath(
//                    startPositionMargin.x, startPositionMargin.y,
//                    stopPositionMargin.x, stopPositionMargin.y
//                )
//                if (!tempPath.isEmpty()) {
//                    val startPosition =
//                        fromFragment.getConnectorPosition(fragmentPosition, line.fromConnectType)
//                    val stopPosition =
//                        toFragment.getConnectorPosition(fragmentPosition, line.toConnectType)
//                    tempPath = pathfinder.makePretty(
//                        tempPath,
//                        OrthogonalJPS.PathPoint(startPosition.x, startPosition.y),
//                        OrthogonalJPS.PathPoint(stopPosition.x, stopPosition.y),
//                        60 + fromFragment.layerRow * 20
//                    )
//                    line.path = tempPath
//                } else {
//                    Log.d("Editor", "Empty Path")
//                }
//            }
//            pathfinder.clearBoard()
//        }
//    }


    private fun hasLines(elementId: String): Boolean {
        var hasLines = false
        for (line in connections) {
            if (line.toElementId == elementId || line.fromElementId == elementId) {
                hasLines = true
            }
        }
        return hasLines
    }
//
//    private fun deleteConnectionsFor(assetId: String) {
//        val linesToDelete = LinkedList<LineLayout>()
//        for (line in connections) {
//            if (line.fromAssetId == assetId) {
//                linesToDelete.add(line)
//            } else if (line.toAssetId == assetId) {
//                linesToDelete.add(line)
//            }
//        }
//        connections.removeAll(linesToDelete)
//        val fragment = videoFragments.get(assetId)
//        fragment?.asset?.choices?.clear()
//        val choicesToDelete = LinkedList<Choice>()
//        for (videoFragment in videoFragments) {
//            for (choice in videoFragment.value.asset.choices) {
//                if (choice.choiceId == assetId) {
//                    choicesToDelete.add(choice)
//                }
//            }
//            videoFragment.value.asset.choices.removeAll(choicesToDelete)
//        }
//    }
//
//    private fun deleteFragment(assetId: String) {
//        val fragmentToDelete = videoFragments.get(assetId)
//        fragmentToDelete?.asset?.let{ asset->
//            onAssetDeleted(asset)
//        }
//        removeView(fragmentToDelete)
//        videoFragments.remove(assetId)
//    }

    fun addAllElementsView(graphElement: TreeVertex<GraphElement>) {
        addGraphView(graphElement.data)
        for (child in graphElement.childs) {
            addAllElementsView(child)
        }
    }

    fun createConnections(
        connections: LinkedList<LineLayout>,
        graphElement: TreeVertex<GraphElement>
    ) {
        for (child in graphElement.childs) {
            connections.add(LineLayout().apply {
                fromConnectType = LineLayout.FORWARD_CONNECTOR
                toConnectType = LineLayout.BACKWARD_CONNECTOR
                fromElementId = graphElement.data.id
                toElementId = child.data.id
            })
            createConnections(connections, child)
        }
    }

    fun prepareId(graphElement: TreeVertex<GraphElement>) {
        graphElement.data.generateId()
        for (child in graphElement.childs) {
            prepareId(child)
        }
    }

    fun setChain(newChain: Chain) {
        this.chain = newChain
        removeAllViews()
        graphViews.clear()

        chain.graph?.let {
            prepareId(it)
            addAllElementsView(it)
            createConnections(connections, it)
            it.getTreeTopElements().forEach {
                graphViews.get(it.data.id)?.let{
                    it.changeStrokeColor(true)
                }
            }
            Log.d("GraphView", "Finded connections: ${connections.size} count")
            connections.forEach { line ->
                Log.d("Connection","Start(${line.fromElementId}) End(${line.toElementId})")
            }
        }
        autoPosition { }
    }

    fun getChain(): Chain {
        return chain
    }

    private fun addGraphView(graphElement: GraphElement) {
        val view = newElement(graphElement)
        addView(view)
        graphViews.put(graphElement.id, view)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        var childWidth: Int
        var childHeight: Int
        var childLeft: Int
        var childTop: Int

        val parentLeft = this.paddingLeft
        val parentTop = this.paddingTop

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE)
                return
            childWidth = child.measuredWidth
            childHeight = child.measuredHeight
            childLeft = parentLeft + 0
            childTop = parentTop + 0

            child.layout(
                childLeft, childTop,
                childLeft + childWidth, childTop + childHeight
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        editorViewSize.x = MeasureSpec.getSize(widthMeasureSpec)
        editorViewSize.y = MeasureSpec.getSize(heightMeasureSpec)
        val scaleForX = editorViewSize.x / editorAreaSize.x.toFloat()
        val scaleForY = editorViewSize.y / editorAreaSize.y.toFloat()
        minScale = max(scaleForX, scaleForY)
        val width = (editorAreaSize.x).toInt()//MeasureSpec.getSize(widthMeasureSpec)
        val height = (editorAreaSize.y).toInt()//MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private val linePen = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorDivider)
        strokeWidth =
            lineWidth * densityDpi
        style = Paint.Style.STROKE
    }

    private val lineBrightPen = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
        strokeWidth =
            lineWidth * densityDpi
        style = Paint.Style.STROKE
    }

    private val obstaclePen = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        strokeWidth =
            lineWidth * densityDpi
        style = Paint.Style.FILL_AND_STROKE
    }

    private fun scaleConnectionsWidth() {
        lineBrightPen.strokeWidth = lineWidth * densityDpi / sqrt(scaleFactor)
        linePen.strokeWidth = lineWidth * densityDpi / sqrt(scaleFactor)
    }

    private val path = Path()

    private fun drawConnections(canvas: Canvas) {
        var brightConnections = LinkedList<LineLayout>()
        Log.d("GraphView", "Draw connections")
        for (line in connections) {
            Log.d("GraphView", "Line from ${line.fromElementId} to ${line.toElementId}")
//            if (selectedFragment.isNotEmpty() && (selectedFragment == line.fromElementId
//                        || selectedFragment == line.toElementId)
//            ) {
//                brightConnections.add(line)
//            } else {
                drawConnection(canvas, line, linePen)
//            }
        }
        for (line in brightConnections) {
            drawConnection(canvas, line, lineBrightPen)
        }
    }

    private fun drawConnection(canvas: Canvas, line: LineLayout, paint: Paint) {
        path.apply {
            reset()
            if (line.path.size > 1) {
                moveTo(line.path[0].x.toFloat(), line.path[0].y.toFloat())
                Log.d("Point", "${line.path[0].x} ${line.path[0].y}")
            }
            for (i in 1 until line.path.size) {
                Log.d("Point", "${line.path[i].x} ${line.path[i].y}")
                if (line.path[i].corner) {
                    quadTo(
                        line.path[i].x.toFloat(), line.path[i].y.toFloat(),
                        line.path[i + 1].x.toFloat(), line.path[i + 1].y.toFloat()
                    )
                } else if (!line.path[i - 1].corner) {
                    lineTo(line.path[i].x.toFloat(), line.path[i].y.toFloat())
                }
            }
        }
        canvas.drawPath(path, paint)
    }

    fun selectFragment(elementId: String?, selectLine: Boolean = false) {
        for (graphView in graphViews) {
            if (graphView.value.data.id == elementId) {
                graphView.value.selectView(true)
                if (selectLine) {
                    selectedFragment = graphView.value.data.id
                    invalidate()
                }
            }
            else {
                graphView.value.selectView(false)
            }
        }
    }

    fun drawObstacles(canvas: Canvas){
        for (i in pathfinder.mBoard!!.indices){
            for (j in pathfinder.mBoard!![i].indices) {
                val cellSize = pathfinder.mTileSize.x.toFloat()
                if (!pathfinder.mBoard!![i][j].walkable) {
                    canvas.drawRect(
                        i * cellSize,
                        j * cellSize,
                        i * cellSize + cellSize,
                        j * cellSize + cellSize,
                        obstaclePen
                    )
                }
            }
        }
    }

    fun drawPathpoints(canvas: Canvas){
        for (line in connections){
            if (line.path.isNotEmpty())
            canvas.drawRect(
                line.path.first.x.toFloat(),
                line.path.first.y.toFloat(),
                line.path.last.x.toFloat(),
                line.path.last.y.toFloat(),
                lineBrightPen
            )
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            drawConnections(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }
}