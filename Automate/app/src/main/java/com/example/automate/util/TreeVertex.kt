package com.example.automate.utils

import java.lang.StringBuilder
import java.util.*
import kotlin.math.max


class TreeVertex<T>(vertexData: T){
    var childs : LinkedList<TreeVertex<T>> = LinkedList<TreeVertex<T>>()
    var data : T = vertexData
    var root : TreeVertex<T> = this

    fun addChild(childData: T): TreeVertex<T>{
        val newVertex = TreeVertex(childData)
        newVertex.root = this.root
        childs.add(newVertex)
        return newVertex
    }

    fun inTree(searchData: T): Boolean{
        if (searchData == data){
            return true
        }
        for(child in childs){
            if (child.inTree(searchData)){
                return true
            }
        }
        return false
    }

    fun findFirst(searchData: T): TreeVertex<T>?{
        if (searchData == data){
            return this
        }
        for (child in childs){
            val searched = child.findFirst(searchData)
            if (searched != null){
                return searched
            }
        }
        return null
    }

    fun getAll(): LinkedList<TreeVertex<T>> {
        val allVertexes = LinkedList<TreeVertex<T>>()
        for (child in childs){
            allVertexes.addAll(child.getAll())
        }
        allVertexes.add(this)
        return allVertexes
    }

    fun getAllAtDepth(depth: Int): LinkedList<TreeVertex<T>> {
        val allVertexes = LinkedList<TreeVertex<T>>()
        allVertexes.addAll(getAllAtDepth(depth, 0))
        return allVertexes
    }

    private fun getAllAtDepth(depth: Int, currentDepth: Int): LinkedList<TreeVertex<T>> {
        val allVertexes = LinkedList<TreeVertex<T>>()
        if (depth == currentDepth) {
            allVertexes.add(this)
        }
        else{
            for (child in childs) {
                allVertexes.addAll(child.getAllAtDepth(depth, currentDepth + 1))
            }
        }
        return allVertexes
    }

    fun getHeight(): Int{
        var height = 0
        for (child in childs){
            height = max(height, child.getHeight())
        }
        height += 1
        return height
    }

    fun getEveryWidth(): IntArray{
        var height = getHeight()
        val widthArray = IntArray(height)
        widthArray!!.fill(0)
        addWidth(widthArray,0)
        return widthArray
    }

    private fun addWidth(widthArray: IntArray, depth: Int){
        for (child in childs){
            child.addWidth(widthArray,depth+1)
        }
        if (depth < widthArray!!.size) {
            widthArray!![depth] += 1
        }
    }


    fun getTreeTopElements():LinkedList<TreeVertex<T>>{
        val topElements = LinkedList<TreeVertex<T>>()
        getTreeTopElements(this, topElements)
        return topElements
    }

    private fun getTreeTopElements(currentRoot: TreeVertex<T>, elements: LinkedList<TreeVertex<T>>){
        if (currentRoot.childs.isEmpty()){
            elements.add(currentRoot)
        }
        else{
            currentRoot.childs.forEach{ child->
                getTreeTopElements(child, elements)
            }
        }
    }

    fun copy(): TreeVertex<T>{
        val copiedVertex = TreeVertex<T>(this.root.data)
        var currentVertex = copy(copiedVertex, this.root)
        if (currentVertex == null) {
            currentVertex = copiedVertex
        }
        return currentVertex
    }

    private fun copy(copiedVertex: TreeVertex<T>, originVertex: TreeVertex<T>): TreeVertex<T>?{
        var currentVertex = if (originVertex == this) copiedVertex else null
        for (child in originVertex.childs) {
            val copiedChild = copiedVertex.addChild(child.data)
            copy(copiedChild, child)?.let{
                currentVertex = it
            }
        }
        return currentVertex
    }

    override fun toString(): String{
        val stringBuilder = StringBuilder()
        toString(this, stringBuilder)
        return stringBuilder.toString()
    }

    private fun toString(currentRoot: TreeVertex<T>, stringBuilder: StringBuilder){
        stringBuilder.append(currentRoot.data.toString())
        currentRoot.childs.forEach{
            toString(it, stringBuilder)
        }
    }
}