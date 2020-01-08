package com.example.grammar.utils

import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import java.util.*
import kotlin.Comparator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt


class OrthogonalJPS(sizeX: Int, sizeY:Int, tileSize: Int) {
    companion object{
        const val DELTA = 0.0001f
    }
    private var SQUARE_ROOT_TWO = sqrt(2f)
    var mBoard: Array<Array<Node>>?
    var mTileSize: Point //size of single tile
    var mBoardSize: Point //size of all area
    private var mVs = Point(0, 0)    // view size
    private var priorityQueue: PriorityQueue<Node> // priority queue for quick pathfinding.
    private val priorityComparator = CustomComparator()

    class PathPoint() {
        var x = 0
        var y = 0
        var corner = false
        constructor(node: Node) : this() {
            x = node.x
            y = node.y
        }
        constructor(x: Int, y: Int) : this() {
            this.x = x
            this.y = y
        }
    }

    class Node() {
        var x = 0
        var y = 0
        var g = 0f
        var h = 0f
        var f = 0f
        var opened = false
        var closed = false
        var walkable = true
        var parent: Node? = null

        fun positionEquals(other: Node): Boolean{
            return other.x == this.x && other.y == this.y
        }

        fun clear(){
            g = 0f
            h = 0f
            f = 0f
            opened = false
            closed = false
            parent = null
        }

        constructor(x: Int, y: Int) : this() {
            this.x = x
            this.y = y
        }

        constructor(node: Node) : this(node.x, node.y) {
            this.g = node.g
            this.h = node.h
            this.f = node.f
            this.opened = node.opened
            this.closed = node.closed
            this.parent = node.parent
            this.walkable = node.walkable
        }

        fun less(other: Node): Boolean {
            return this.f < other.f
        }

        fun more(other: Node): Boolean {
            return this.f > other.f
        }
    }

    init {
        mVs.x = sizeX
        mVs.y = sizeY

        mTileSize = Point(tileSize, tileSize)
        mBoardSize = Point(sizeX / mTileSize.x, sizeY / mTileSize.y)
        mBoard = Array<Array<Node>>(mBoardSize.x){ Array(mBoardSize.y){Node()}}
        for (i in 0 until mBoardSize.x){
            for (j in 0 until mBoardSize.y){
                mBoard!![i][j].x = i
                mBoard!![i][j].y = j

            }
        }
        priorityQueue = PriorityQueue(10, priorityComparator)
    }

    class CustomComparator : Comparator<Node> {
        override fun compare(c1: Node, c2: Node): Int {
            return if (abs(c1.f - c2.f) < DELTA) 0 else if (c1.f - c2.f < 0) -1 else 1
        }
    }

    fun addObstacle(left: Int, top: Int, right: Int, bottom: Int) {
        val leftTile = left / mTileSize.x
        val topTile = top / mTileSize.y
        val rightTile = right / mTileSize.x
        val bottomTile = bottom / mTileSize.y
        //top side of obstacle
        for(i in leftTile..rightTile){
            if (valid(i, topTile)){
                mBoard!![i][topTile].walkable = false
            }
        }
        //bottom side of obstacle
        for(i in leftTile..rightTile){
            if (valid(i, bottomTile)){
                mBoard!![i][bottomTile].walkable = false
            }
        }
        //left side of obstacle
        for(i in topTile+1 until bottomTile){
            if (valid(leftTile, i)){
                mBoard!![leftTile][i].walkable = false
            }
        }
        //right side of obstacle
        for(i in topTile+1 until bottomTile){
            if (valid(rightTile, i)){
                mBoard!![rightTile][i].walkable = false
            }
        }
    }

    fun clearBoard(){
        for (i in 0 until mBoardSize.x){
            for (j in 0 until mBoardSize.y){
                mBoard!![i][j].clear()
            }
        }
    }

    fun removeBoard(){
        mBoard = null
    }

    fun clearObstacles() {
        for(i in 0 until mBoardSize.x){
            for (j in 0 until mBoardSize.y){
                mBoard!![i][j].walkable = true
            }
        }
    }

    private var startNode = Node()
    private var endNode = Node()
    private var startPosition = PathPoint()
    private var endPosition = PathPoint()

    fun findPath(startX: Int, startY: Int, endX: Int, endY: Int): LinkedList<PathPoint> {
        mBoard?:return LinkedList()
        var currentNode : Node
        startNode = Node()
        endNode = Node()
        priorityQueue.clear()
        if (!valid(startX / mTileSize.x,startY / mTileSize.y) ||
            !valid(endX / mTileSize.x,endY / mTileSize.y) ){
            return LinkedList()
        }
        startPosition.x = startX
        startPosition.y = startY
        endPosition.x = endX
        endPosition.y = endY

        startNode = mBoard!![startX / mTileSize.x][startY / mTileSize.y]
        endNode = mBoard!![endX / mTileSize.x][endY / mTileSize.y]

        startNode.g = 0f
        startNode.f = 0f

        startNode.opened = true
        priorityQueue.add(startNode)

        while (!priorityQueue.isEmpty()) {
            currentNode = priorityQueue.poll()!!
            currentNode.closed = true
            if (currentNode.positionEquals(endNode)) {
                return tilesToCoordinates( backtrace(endNode) )
            }
            findSuccessors(currentNode)
        }
        return LinkedList()
    }

    private fun backtrace(node: Node): LinkedList<PathPoint> {
        val path = LinkedList<PathPoint>()
        path.add(PathPoint(node))
        var currentNode = Node(node)
        while (currentNode.parent != null) {
            currentNode = currentNode.parent!!
            path.addFirst(PathPoint(currentNode))
        }
        return path
    }

    private fun tilesToCoordinates(pathPoints: LinkedList<PathPoint>): LinkedList<PathPoint>{
        for (point in pathPoints) {
            point.x = point.x * mTileSize.x
            point.y = point.y * mTileSize.y
        }
        return pathPoints
    }

    fun makePretty(pathPoints: LinkedList<PathPoint>, startPoint: PathPoint, endPoint: PathPoint, margin: Int = mTileSize.x): LinkedList<PathPoint>{
        if (pathPoints.size > 1) {
            var i = pathPoints.size-1
            var value = pathPoints[i].y
            while (i >= 0 && pathPoints[i].y == value){
                pathPoints[i].y = endPoint.y - margin
                i--
            }
            i = pathPoints.size-1
            value = pathPoints[i].x
            while (i >= 0 && pathPoints[i].x == value){
                pathPoints[i].x = endPoint.x
                i--
            }

            i = 0
            value = pathPoints[i].y
            while (i < pathPoints.size && pathPoints[i].y == value){
                pathPoints[i].y = startPoint.y + margin
                i++
            }
            i = 0
            value = pathPoints[i].x
            while (i < pathPoints.size && pathPoints[i].x == value){
                pathPoints[i].x = startPoint.x
                i++
            }

            pathPoints.addFirst(startPoint)
            pathPoints.addLast(endPoint)
        }

        return roundCorners(removeExtraPoints(pathPoints))
    }

    private fun removeExtraPoints(path: LinkedList<PathPoint>): LinkedList<PathPoint>{
        val newPath = LinkedList<PathPoint>()
        if (path.isEmpty()){
            return newPath
        }
        newPath.add(path.first)
        for (i in 1 until path.size-1){
            val firstDx = path[i].x - path[i - 1].x
            val firstDy = path[i].y - path[i - 1].y
            val secondDx = path[i + 1].x - path[i].x
            val secondDy = path[i + 1].y - path[i].y
            if (firstDx != 0 && secondDy != 0){
                newPath.addLast(PathPoint(path[i].x, path[i].y))
            }
            else if (firstDy != 0 && secondDx != 0){
                newPath.addLast(PathPoint(path[i].x, path[i].y))
            }
        }
        newPath.addLast(path.last)
        return newPath
    }


    private fun roundCorners(path: LinkedList<PathPoint>, radius: Int = mTileSize.x): LinkedList<PathPoint>{
        val newPath = LinkedList<PathPoint>()
        if (path.isEmpty()){
            return newPath
        }
        newPath.add(path.first)
        for (i in 1 until path.size-1){
            val firstDx = path[i].x - path[i - 1].x
            val firstDy = path[i].y - path[i - 1].y
            val secondDx = path[i + 1].x - path[i].x
            val secondDy = path[i + 1].y - path[i].y
            if (firstDx != 0 && secondDy != 0){
                var newX = 0
                var newY = 0
                if (firstDx < 0){
                    newX = path[i].x + radius
                    if (newX < path[i-1].x){
                        newPath.addLast(PathPoint(newX, path[i].y))
                    }
                }
                else{
                    newX = path[i].x - radius
                    if (newX > path[i-1].x){
                        newPath.addLast(PathPoint(newX, path[i].y))
                    }
                }
                newPath.addLast(path[i].apply{corner = true})
                if (secondDy < 0){
                    newY = path[i].y - radius
                    if (newY > path[i+1].y){
                        newPath.addLast(PathPoint(path[i].x, newY))
                    }
                }
                else{
                    newY = path[i].y + radius
                    if (newY < path[i+1].y){
                        newPath.addLast(PathPoint(path[i].x, newY))
                    }
                }
            }
            else if (firstDy != 0 && secondDx != 0){
                var newX = 0
                var newY = 0
                if (firstDy < 0){
                    newY = path[i].y + radius
                    if (newY < path[i-1].y){
                        newPath.addLast(PathPoint(path[i].x, newY))
                    }
                }
                else{
                    newY = path[i].y - radius
                    if (newY > path[i-1].y){
                        newPath.addLast(PathPoint(path[i].x, newY))
                    }
                }
                newPath.addLast(path[i].apply{corner = true})
                if (secondDx < 0){
                    newX = path[i].x - radius
                    if (newX > path[i+1].x){
                        newPath.addLast(PathPoint(newX, path[i].y))
                    }
                }
                else{
                    newX = path[i].x + radius
                    if (newX < path[i+1].x){
                        newPath.addLast(PathPoint(newX, path[i].y))
                    }
                }
            }
            else{
                //newPath.addLast(path[i])
            }
        }
        newPath.addLast(path.last)
        return newPath
    }

    private fun replaceEdgesWithGivenPositions(pathPoints: LinkedList<PathPoint>): LinkedList<PathPoint>{
        pathPoints[0].x = startPosition.x
        pathPoints[0].y = startPosition.y
        pathPoints[pathPoints.size-1].x = endPosition.x
        pathPoints[pathPoints.size-1].y = endPosition.y
        return pathPoints
    }

    private fun jump(x: Int, y: Int, px: Int, py: Int): PathPoint?{
        val dx = x - px
        val dy = y - py

        if (!isWalkable(x, y)) {
            return null
        }

        if (mBoard!![x][y].positionEquals(endNode)) {
            return PathPoint(x, y)
        }

        if (dx != 0) {
            if ((isWalkable(x,y - 1) && !isWalkable(x - dx, y - 1)) ||
                (isWalkable(x, y + 1) && !isWalkable(x - dx, y + 1))) {
                return PathPoint(x, y)
            }
        }
        else if (dy != 0) {
            if ((isWalkable(x - 1, y) && !isWalkable(x - 1, y - dy)) ||
                (isWalkable(x + 1, y) && !isWalkable(x + 1, y - dy))) {
                return PathPoint(x, y)
            }
            val jumpPoint1 = jump(x + 1, y, x, y)
            val jumpPoint2 = jump(x - 1, y, x, y)
            //When moving vertically, must check for horizontal jump points
            if (jumpPoint1 != null || jumpPoint2 != null) {
                return PathPoint(x, y)
            }
        }
        else {
            Log.e("OrthogonalJPS","Only horizontal and vertical movements are allowed")
        }
        //we should not be here, but try to jump
        return jump(x + dx, y + dy, x, y)
    }

    private fun findSuccessors(node: Node) {
        val endX = endNode.x
        val endY = endNode.y
        var jumpPoint: PathPoint? = null
        lateinit var jumpNode: Node
        var ng = 0f
        var jx = 0
        var jy = 0

        val neighbours = findNeighbors(node)
        for (n in neighbours) {
            jumpPoint = jump(n.x, n.y, node.x, node.y)
            if (jumpPoint != null) {
                jx = jumpPoint.x
                jy = jumpPoint.y
                jumpNode = mBoard!![jx][jy]
                if (jumpNode.closed) {
                    continue
                }
                ng = node.g + octile(abs(jx - node.x), abs(jy - node.y))
                if (!jumpNode.opened || ng < jumpNode.g) {
                    jumpNode.g = ng
                    if (abs(jumpNode.h) < DELTA)
                        jumpNode.h = heuristic(abs(jx - endX), abs(jy - endY))
                    jumpNode.f = jumpNode.g + jumpNode.h
                    jumpNode.parent = node
                    if (!jumpNode.opened) {
                        jumpNode.opened = true
                        priorityQueue.add(jumpNode)
                    } else {
                        for (value in priorityQueue){
                            if (value.positionEquals(jumpNode)){
                                priorityQueue.remove(value)
                                break
                            }
                        }
                        priorityQueue.add(jumpNode)

                    }
                }
            }
        }
    }

    private fun heuristic(dx: Int, dy: Int): Float{
        return dx.toFloat() + dy.toFloat()
    }

    private fun octile(dx: Int, dy: Int): Float {
        return if (dx < dy) (SQUARE_ROOT_TWO-1f) * dx.toFloat() + dy.toFloat()
        else (SQUARE_ROOT_TWO-1f) * dy.toFloat() + dx.toFloat()
    }

    private fun findNeighbors(node: Node): LinkedList<PathPoint> {
        var parent = node.parent
        var dx = 0
        var dy = 0
        val neighbors = LinkedList<PathPoint>()
        if (parent != null){
            val x = node.x
            val y = node.y
            val px = parent.x
            val py = parent.y
            dx = (x - px) / max(abs(x-px),1)
            dy = (y - py) / max(abs(y-py), 1)
            if (dx != 0){
                if (isWalkable(x, y - 1)) {
                    neighbors.add(PathPoint(x, y - 1))
                }
                if (isWalkable(x, y + 1)) {
                    neighbors.add(PathPoint(x, y + 1))
                }
                if (isWalkable(x + dx, y)) {
                    neighbors.add(PathPoint(x + dx, y))
                }
            }
            else if (dy != 0){
                if (isWalkable(x - 1, y)) {
                    neighbors.add(PathPoint(x - 1, y))
                }
                if (isWalkable(x + 1, y)) {
                    neighbors.add(PathPoint(x + 1, y))
                }
                if (isWalkable(x, y + dy)) {
                    neighbors.add(PathPoint(x, y + dy))
                }
            }
        }
        //all neighbors
        else{
            val neighborNodes = getNeighbors(node)
            for (n in neighborNodes){
                neighbors.add(PathPoint(n))
            }
        }
        return neighbors
    }

    private fun getNeighbors(node: Node): LinkedList<Node>{
        val neighbors = LinkedList<Node>()
        val x = node.x
        val y = node.y
        if (isWalkable(x, y-1)){
            neighbors.add(mBoard!![x][y-1])
        }
        if (isWalkable(x+1, y)){
            neighbors.add(mBoard!![x+1][y])
        }
        if (isWalkable(x, y+1)){
            neighbors.add(mBoard!![x][y+1])
        }
        if (isWalkable(x-1, y)){
            neighbors.add(mBoard!![x-1][y])
        }
        return neighbors
    }

    private fun isWalkable(x: Int, y: Int): Boolean{
        return valid(x, y) && mBoard!![x][y].walkable
    }

    private fun valid(x: Int, y: Int): Boolean {
        return x >= 0 && x < mBoardSize.x && y >= 0 && y < mBoardSize.y
    }
}