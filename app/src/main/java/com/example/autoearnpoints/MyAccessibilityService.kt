package com.example.autoearnpoints

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.compose.ui.geometry.Rect

class MyAccessibilityService : AccessibilityService() {

    val tartget_package = "com.nn.accelerator.community"
    private var currentState = CurrentState.WILL_GO_AD
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("===","ServiceConnected!---------------------------")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.e("onAccessibilityEvent","type:${event?.eventType} currentState:$currentState")

        when(currentState){
            CurrentState.WILL_GO_AD-> event?.source?.apply {
                val go = this@MyAccessibilityService.rootInActiveWindow.findAccessibilityNodeInfosByText("去完成")
                if(go.isEmpty()){
                    Toast.makeText(this@MyAccessibilityService,"今天任务全部完成了",Toast.LENGTH_LONG).show()
                    return
                }
                Log.d("===","还有${go.size}个大任务")
                if(go[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                    currentState = CurrentState.OTHERS
                    Log.d("===","click go ad!")
                }
                recycle()
            }

            CurrentState.PLAYING_AD -> {
                event?.source?.apply {
                    windows.forEach {windowInfo ->
                        run {
                            val root = windowInfo.root
                            findCloseBtn(root)
                            webviewList.clear()
                            findWebViewNode(root)
                            findAndPerformWebViewCloseBtn()
                        }
                    }

                    recycle()
                }
            }

            CurrentState.OTHERS -> {
                event?.source?.apply {
                    val exit = this@MyAccessibilityService.rootInActiveWindow?.findAccessibilityNodeInfosByText("退出")
                    if(exit?.isNotEmpty() == true) {
                        exit[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    currentState = CurrentState.PLAYING_AD
                    recycle()
                }
            }

        }
    }

    val CLOSE_BTN_BOUND = android.graphics.Rect(669,-31,1059,180)
    private fun findAndPerformWebViewCloseBtn() {
        if(webviewList.isEmpty()) return
        for(webView in webviewList){
            for(i in 0 until webView.childCount){
                val child = webView.getChild(i)
                val location = android.graphics.Rect()
                child.getBoundsInScreen(location)
                if(child.text .contains("跳过")){
                    child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                Log.d("===",location.flattenToString())
                if(child.className == "android.widget.ImageView" && CLOSE_BTN_BOUND.contains(location)){
                    Log.d("===","find out webview close btn")
                    currentState = CurrentState.WILL_GO_AD
                    child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }


    }

    private fun findCloseBtn(nodeInfo: AccessibilityNodeInfo?):AccessibilityNodeInfo? {
        if(nodeInfo == null){
            return null
        }

//        val node = nodeInfo.findAccessibilityNodeInfosByViewId("$tartget_package:id/tt_video_ad_close_layout")
//        if(node.isNotEmpty()){
//            node[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        }
        for (i in 0 until nodeInfo.childCount){
            val child = nodeInfo.getChild(i) ?: continue
            if(child.isClickable && child.className == "android.widget.RelativeLayout"){
                Log.d("===","find out close btn!")
                currentState = CurrentState.WILL_GO_AD
                child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                if(nodeInfo.childCount > 0){
                    findCloseBtn(nodeInfo.getChild(i))
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.e("===","ServiceInterrupt!-----------")
    }

    var webviewList = ArrayList<AccessibilityNodeInfo>()
    private fun findWebViewNode(rootNode: AccessibilityNodeInfo?):AccessibilityNodeInfo? {
        if(rootNode == null) return null
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            if ("android.webkit.WebView" == child.className) {
                Log.d("findWebViewNode--", "找到webView")
                webviewList.add(child)
            }
            if (child.childCount > 0) {
                return findWebViewNode(child)
            }
        }
        return null
    }
}
enum class CurrentState {
    WILL_GO_AD,
    PLAYING_AD,
    OTHERS
}