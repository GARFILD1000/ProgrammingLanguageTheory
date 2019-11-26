package com.example.automate.model
import com.example.automate.utils.OrthogonalJPS
import java.util.*

class LineLayout {
    var fromConnectType = 0
    var toConnectType = 0
    var fromElementId = ""
    var toElementId = ""
    var path: LinkedList<OrthogonalJPS.PathPoint> = LinkedList()

    companion object{
        const val NO_TYPE = 0
        const val BACKWARD_CONNECTOR = 3
        const val FORWARD_CONNECTOR = 4
    }

    fun clear(){
        fromConnectType = 0
        toConnectType = 0
        fromElementId = ""
        toElementId = ""
        path.clear()
    }

    fun hasStart(): Boolean{
        return fromConnectType != 0 && fromElementId.isNotEmpty()
    }

    fun hasEnd(): Boolean{
        return toConnectType != 0 && toElementId.isNotEmpty()
    }
}