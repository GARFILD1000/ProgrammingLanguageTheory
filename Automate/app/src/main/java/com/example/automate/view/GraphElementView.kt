package com.example.automate.view

import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import android.graphics.Bitmap
import com.example.automate.R
import com.example.automate.model.GraphElement
import com.example.automate.model.LineLayout
import kotlinx.android.synthetic.main.view_graph_element.view.*
import kotlin.math.abs


class GraphElementView(context: Context, var data: GraphElement): LinearLayout(context) {
    private var thumbLoaded: Boolean = false
    var onCreateFragment: (GraphElementView) -> Unit = {}
    var onFragmentCreated: (assetId: String) -> Unit = {}

    var onDragConnection: (newPosition: PointF) -> Unit = {}
    var onCreateConnection: (fromElement: GraphElement, fromConnectType: Int) -> Unit = {_,_->}
    var onDragConnectionFinished: (newPosition: PointF) -> Unit = {}

    var onClicked: (dataId: String) -> Unit = {}
    var onTitleChanged: (assetId: String, newTitle: String) -> Unit = {_,_->}
    var onCreatePopup: (assetId: String) -> Unit = {}
    var onAddVideo: (assetId: String) -> Unit = {}
    var onVideoAdded: (assetId: String, newVideoUrl: String) -> Unit = {_,_->}

    lateinit var layoutInflater: LayoutInflater
    var layoutCreated = false

    private lateinit var view: View
    private var strokeColorChanged = false
    private var lastPoint = PointF().apply{x = 0f; y = 0f}
    private var startPoint = PointF().apply{x = 0f; y = 0f}
    private var movingPosition = PointF().apply{x = 0f; y = 0f}

    var pathfinderPositionX = 0
    var pathfinderPositionY = 0
    var layerColumn = 0
    var layerRow = 0


    init {
        initView(context)
    }

    inner class EditorGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
        }
    }

    private var gestureDetector = GestureDetector(context, EditorGestureListener())

    fun changeStrokeColor(stateSelected: Boolean) {
        if (stateSelected && !strokeColorChanged) {
            view.parentContainer.background = ContextCompat.getDrawable(context, R.drawable.shape_frame_rounded_selected)
//            view.dividerCreateButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
//            view.createButton.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
            view.connectBackward.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            view.connectForward.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            strokeColorChanged = true
        }
        if (!stateSelected && strokeColorChanged){
            view.parentContainer.background = ContextCompat.getDrawable(context, R.drawable.shape_frame_rounded_unselected)
//            view.dividerCreateButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDivider))
//            view.createButton.setColorFilter(ContextCompat.getColor(context, R.color.colorDivider))
            view.connectBackward.setColorFilter(ContextCompat.getColor(context,R.color.colorPrimary))
            view.connectForward.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            strokeColorChanged = false
        }
        requestLayout()
    }

    fun selectView(stateSelected: Boolean){
        if (stateSelected){
            view.parentContainer.background = ContextCompat.getDrawable(context, R.drawable.shape_frame_rounded_accent)
            view.connectBackward.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
            view.connectForward.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
        }
        else{
            view.parentContainer.background = ContextCompat.getDrawable(context, R.drawable.shape_frame_rounded_unselected)
            view.connectBackward.setColorFilter(ContextCompat.getColor(context,R.color.colorDivider))
            view.connectForward.setColorFilter(ContextCompat.getColor(context, R.color.colorDivider))
        }
    }

    fun shadow(stateShadowed: Boolean){
        if (stateShadowed) {
            view.parentContainer.alpha = 0.5f
        } else {
            view.parentContainer.alpha = 1f
        }
        requestLayout()
    }

    var connectionStarted = false

    var textChanged = false

    private fun initView(context: Context) {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = layoutInflater.inflate(R.layout.view_graph_element, this, false)
        view.parentContainer.setOnClickListener{
            onCreatePopup(data.id)
        }
        view.dataTextView.setText(data.data)

        view.parentContainer.setOnClickListener{
            onClicked(data.id)
        }

        view.parentContainer.setOnTouchListener{ _, event->
            val touchSlop = ViewConfiguration.get(context).getScaledTouchSlop()
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    lastPoint.x = event.rawX
                    lastPoint.y = event.rawY
                    startPoint.x = event.rawX
                    startPoint.y = event.rawY
                    connectionStarted = false
//                    onSelect(asset.assetId)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!connectionStarted && (abs(startPoint.x - event.rawX) > touchSlop && abs(startPoint.y - event.rawY) > touchSlop)){
                        connectionStarted = true
                        onCreateConnection(data, LineLayout.FORWARD_CONNECTOR)
                        movingPosition.x = x - width //- (event.rawX - startPoint.x)
                        movingPosition.y = y //- (event.rawY - startPoint.y)
                    }
                    val pointX = event.rawX
                    val pointY = event.rawY
                    val newX = movingPosition.x - (lastPoint.x - pointX) / (parent as ViewGroup).scaleX
                    val newY = movingPosition.y - (lastPoint.y - pointY) / (parent as ViewGroup).scaleY
                    movingPosition.x = newX
                    movingPosition.y = newY
                    onDragConnection(movingPosition)
//                    if ((newX > 0 || newX > x) && ((newX + measuredWidth < (parent as ViewGroup).measuredWidth) || newX < x)) {
//                        x = newX
//                        moved = true
//                    }
//                    if ((newY > 0 || newY > y) && ((newY + measuredHeight < (parent as ViewGroup).measuredHeight) || newY < y)) {
//                        y = newY
//                        moved = true
//                    }
                    lastPoint.x = pointX
                    lastPoint.y = pointY

                }
                MotionEvent.ACTION_UP -> {
                    if (abs(startPoint.x - event.rawX) < touchSlop && abs(startPoint.y - event.rawY) < touchSlop) {
                        view.parentContainer.performClick()
                    }
                    onDragConnectionFinished(movingPosition)
                }
            }
            true
        }

//        view.createButton.setOnClickListener {
//            onCreateFragment(this)
//        }


        view.viewTreeObserver.addOnGlobalLayoutListener {
            if (!layoutCreated) {
                onFragmentCreated(data.id)
                layoutCreated = true
            }
        }
        addView(view)
    }

    fun contains(point: PointF): Boolean {
        var contains = false
        if (point.x >= x && point.y >= y && point.x <= (x + measuredWidth) && point.y <= (y + measuredHeight)) {
            contains = true
        }
        return contains
    }

    fun contains(pointX: Float, pointY: Float): Boolean {
        var contains = false
        if (pointX >= x && pointY >= y && pointX <= (x + measuredWidth) && pointY <= (y + measuredHeight)) {
            contains = true
        }
        return contains
    }

    fun getConnectorType(point: PointF): Int {
        val localPoint = PointF(point.x - x, point.y - y)
        var connectorType = LineLayout.NO_TYPE
        val radius = 40
        if (localPoint.x > view.connectForward.x - radius && localPoint.y > view.connectForward.y - radius &&
            localPoint.x - radius < view.connectForward.x + view.connectForward.measuredWidth &&
            localPoint.y - radius < view.connectForward.y + view.connectForward.measuredHeight
        ) {
            connectorType = LineLayout.FORWARD_CONNECTOR
        } else if (localPoint.x > view.connectBackward.x - radius && localPoint.y > view.connectBackward.y - radius &&
            localPoint.x - radius < view.connectBackward.x + view.connectBackward.measuredWidth &&
            localPoint.y - radius < view.connectBackward.y + view.connectBackward.measuredHeight
        ) {
            connectorType = LineLayout.BACKWARD_CONNECTOR
        }
        return connectorType
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val child = if (childCount > 0) getChildAt(childCount - 1) else null
        if (child != null) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            width = child.measuredWidth
            height = child.measuredHeight
        }
        setMeasuredDimension(width, height)
    }

    fun getConnectorPosition(type: Int, margin: Int = 0): Point {
        return when (type) {
            LineLayout.BACKWARD_CONNECTOR -> {
                Point(
                    (x + view.connectBackward.x + view.connectBackward.measuredWidth / 2).toInt() - margin,
                    (y + view.connectBackward.y + view.connectBackward.measuredHeight / 2).toInt()
                )
            }
            LineLayout.FORWARD_CONNECTOR -> {
                Point(
                    (x + view.connectForward.x + view.connectForward.measuredWidth / 2).toInt() + margin,
                    (y + view.connectForward.y + view.connectForward.measuredHeight / 2).toInt()
                )
            }
            else -> {
                Point(0, 0)
            }
        }
    }

    fun getConnectorPosition(viewPosition: Point, type: Int, margin: Int = 0): Point {
        return when (type) {
            LineLayout.BACKWARD_CONNECTOR -> {
                Point(
                    (viewPosition.x + view.connectBackward.x + view.connectBackward.measuredWidth / 2).toInt(),
                    (viewPosition.y + view.connectBackward.y + view.connectBackward.measuredHeight / 2).toInt() - margin
                )
            }
            LineLayout.FORWARD_CONNECTOR -> {
                Point(
                    (viewPosition.x + view.connectForward.x + view.connectForward.measuredWidth / 2).toInt(),
                    (viewPosition.y + view.connectForward.y + view.connectForward.measuredHeight / 2).toInt() + margin
                )
            }
            else -> {
                Point(0, 0)
            }
        }
    }

    fun getSize(margin: Int = 0): Point =
        Point().apply{
            x = view.measuredWidth + margin * 2
            y = view.measuredHeight + margin * 2
        }

    fun getPosition(margin: Int = 0): Point =
        Point(x.roundToInt() + margin, y.roundToInt() + margin)

    fun getBitmap(): Bitmap{
        //Define a bitmap with the same size as the view
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        canvas.drawColor(Color.TRANSPARENT)
        val bgDrawable = view.background
        if (bgDrawable != null)
        //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        else
        //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.TRANSPARENT)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }
}

