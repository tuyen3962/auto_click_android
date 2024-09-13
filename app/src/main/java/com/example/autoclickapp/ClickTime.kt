package com.example.autoclickapp

class ClickTime(hour: Int, minute: Int, second: Int, ms: Int) {
    val hour: Int = hour
    val minute: Int = minute
    val second: Int = second
    val ms: Int = ms

    fun isSameTime(clickTime: ClickTime) : Boolean {
        if(clickTime.hour == hour && clickTime.minute == minute && clickTime.second == second) {
            if(clickTime.ms == ms) {
                return true
            } else {
                val start = clickTime.ms
                val end = clickTime.ms + 100
                return ms > start && ms < end
            }
        }

        return false
    }

    fun increaseTime(millisecond: Int) : ClickTime {
        var newMs = ms+millisecond
        var newSecond = second
        var newMinute = minute
        var newHour = hour
        if(newMs >= 1000) {
            newMs -= 1000
            newSecond += 1
            if(newSecond >= 60) {
                newMinute += 1
                newSecond = 0
                if(newMinute >= 60) {
                    newMinute = 0
                    newHour += 1
                    if(newHour >= 23) {
                        newHour = 0
                    }
                }
            }
        }
        return ClickTime(newHour, newMinute, newSecond, newMs)
    }

    override fun toString(): String {
        val msText = (ms / 100) * 100
        return String.format("%s:%s:%s:%s", getTimeFormat(hour), getTimeFormat(minute), getTimeFormat(second), getMsFormat(msText))
    }

    private fun getTimeFormat(time: Int) : String {
        if(time < 10) {
            return String.format("0%d", time)
        } else {
            return time.toString()
        }
    }

    private fun getMsFormat(time: Int) : String {
        if(time > 0) {
            return String.format("%d", time)
        } else {
            return "000"
        }
    }



}