package org.tensorflow.lite.examples.posenet

import android.content.Context
import android.media.ImageReader
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.TextToSpeech.QUEUE_ADD
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Person
import org.tensorflow.lite.examples.posenet.lib.Position
import kotlin.math.roundToInt

class Physio(context: Context) {

    val tts: TextToSpeech

    init {
        tts = TextToSpeech(context,
            OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {

                }
            })
    }

    val MIN_AMPLITUDE = 40
    val REP_THRESHOLD = 0.8
    val MIN_CONFIDENCE = 0.25

    var count = 0
    var angle = 0
    var armRaiseCount = 0

    var first = true
    var goal = 1
    var prev_y = 0
    var prev_dy = 0
    var top = 0
    var bottom = 0

    private var exercise_pos = 0
    private var prev_exercise_pos = 0

    fun start(){
        first = true
    }

    fun OnSquat(person: Person) : Int {

        // Simple repetition count using only the nose as key point
        if (person.keyPoints[BodyPart.NOSE.ordinal].score >= MIN_CONFIDENCE) {
            var y = 10000 - person.keyPoints[BodyPart.NOSE.ordinal].position.y
            var dy = y - prev_y

            if(first){
                speak("Exercise One. Squat.")
            }

            if (!first) {
                if (bottom != 1000000 && top != 0) {
                    if (goal == 1 && dy > 0 && (y - bottom) > (top - bottom) * REP_THRESHOLD) {
                        if (top - bottom > MIN_AMPLITUDE) {
                            count++
                            goal = -1
                            speak(count.toString())
                        }
                    }
                    else if (goal == -1 && dy < 0 && (top - y) > (top - bottom) * REP_THRESHOLD) {
                        goal = 1
                    }
                }


                if (dy < 0 && prev_dy >= 0 && prev_y - bottom > MIN_AMPLITUDE) {
                    top = prev_y
                }
                else if (dy > 0 && prev_dy <= 0 && top - prev_y > MIN_AMPLITUDE) {
                    bottom = prev_y
                }
            }

            first = false
            prev_y = y
            prev_dy = dy
        }

        return count
    }

    fun OnFlexion(person: Person) : Int {

        if (person.keyPoints[BodyPart.RIGHT_SHOULDER.ordinal].score >= MIN_CONFIDENCE
            && person.keyPoints[BodyPart.RIGHT_WRIST.ordinal].score >= MIN_CONFIDENCE) {

            var elbow = person.keyPoints[BodyPart.RIGHT_ELBOW.ordinal].position
            var shoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.ordinal].position
            var wrist = person.keyPoints[BodyPart.RIGHT_WRIST.ordinal].position


            if(first){
                speak("Exercise Two. Right arm flexion.")
            }

            angle = calculateAngle(shoulder, elbow, wrist)

            first = false
        }

        return angle

    }

    fun OnArmRaise(person: Person) : Int {
        if ((person.keyPoints[BodyPart.NOSE.ordinal].score >= MIN_CONFIDENCE )
                && (person.keyPoints[BodyPart.RIGHT_WRIST.ordinal].score>=MIN_CONFIDENCE)
                && (person.keyPoints[BodyPart.LEFT_WRIST.ordinal].score>=MIN_CONFIDENCE)) {

            val rightArm = person.keyPoints[BodyPart.RIGHT_WRIST.ordinal].position
            val leftArm = person.keyPoints[BodyPart.LEFT_WRIST.ordinal].position
            val nose =  person.keyPoints[BodyPart.NOSE.ordinal].position

            if(first){
                speak("Exercise Three. Arms Raise.")
            }


            if ((rightArm.y > nose.y ) && (leftArm.y > nose.y )){
                exercise_pos = 0
                if (prev_exercise_pos != exercise_pos){
                    armRaiseCount++
                    speak(armRaiseCount.toString())
                }

            }
            else if ((rightArm.y < nose.y ) && (leftArm.y < nose.y )){
                exercise_pos = 1
            }

            prev_exercise_pos = exercise_pos;
            first = false
        }
        return armRaiseCount
    }

    // input p1, p2, p3
    // output result = atan2(p1.y - p2.y, p1.x - p2.x) - atan2(p3.y - p2.y, p3.x - p2.x);
    private fun calculateAngle(p1: Position, p2: Position, p3: Position): Int {
        return ((kotlin.math.atan2((p1.y - p2.y).toDouble(), (p1.x - p2.x).toDouble()) -
                kotlin.math.atan2((p3.y - p2.y).toDouble(), (p3.x - p2.x).toDouble())) *
                (180 / Math.PI)).roundToInt()
    }

    fun speak(word: String) {
        tts.speak(word, QUEUE_ADD, null)
    }

    fun Reset() {
        count = 0
    }
}