package com.anwesh.uiprojects.circleshifterrotlineview

/**
 * Created by anweshmishra on 06/02/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val circles : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.8f
val foreColor : Int = Color.parseColor("#311B92")
val backColor : Int = Color.parseColor("#BDBDBD")
val rFactor : Float = 5.9f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Paint.setStyle(w : Float, h : Float) {
    strokeWidth = Math.min(w, h) / strokeFactor
    strokeCap = Paint.Cap.ROUND
    color = foreColor
    style = Paint.Style.STROKE
}

fun Canvas.drawShiftedCircleLine(r : Float, br : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    val x : Float = r * sc1
    for (j in 0..1) {
        save()
        translate(0f, 0f)
        rotate(45f * j * sc2)
        drawLine(-br, 0f, -br + x, 0f, paint)
        drawCircle(0f, 0f, br, paint)
        restore()
    }
}

fun Canvas.drawCSRLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val br : Float = size / rFactor
    val rotDeg : Float = 360f / circles
    paint.setStyle(w, h)
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..circles) {
        save()
        rotate(rotDeg * j)
        drawShiftedCircleLine(size, br, sc1.divideScale(j, circles), sc2.divideScale(j, circles), paint)
        restore()
    }
    restore()
}

class CircleShifterRotLineView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, circles, circles)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CSRLNode(var i : Int, val state : State = State()) {

        private var next : CSRLNode? = null
        private var prev : CSRLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = CSRLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCSRLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : CSRLNode {
            var curr : CSRLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class CircleShifterRotLine(var i : Int) {
        private val root : CSRLNode = CSRLNode(0)
        private var curr : CSRLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CircleShifterRotLineView) {

        private val animator : Animator = Animator(view)
        private val csrl : CircleShifterRotLine = CircleShifterRotLine(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            csrl.draw(canvas, paint)
            animator.animate {
                csrl.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            csrl.startUpdating {
                animator.start()
            }
        }
    }
}