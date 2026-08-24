package com.igris.assistant

import com.igris.assistant.brain.TaskClassifier
import com.igris.assistant.core.TaskClass
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierTest {
    @Test fun intents() {
        assertEquals(TaskClass.CALCULATION, TaskClassifier.understand("calculate 5*8").task)
        assertEquals(TaskClass.DEVICE_CONTROL, TaskClassifier.understand("flashlight on").task)
        assertEquals(TaskClass.TIMER, TaskClassifier.understand("set a 5 minute timer").task)
        assertEquals(TaskClass.REMINDER, TaskClassifier.understand("remind me at 8:00 pm").task)
        assertEquals(TaskClass.MEMORY, TaskClassifier.understand("remember my perfume price is 1200").task)
        assertEquals(TaskClass.PLANNING, TaskClassifier.understand("help me plan a youtube channel").task)
        assertEquals(TaskClass.KNOWLEDGE, TaskClassifier.understand("what is in my perfume pricing document").task)
        assertEquals(TaskClass.BRIEFING, TaskClassifier.understand("good morning IGRIS").task)
    }
}
