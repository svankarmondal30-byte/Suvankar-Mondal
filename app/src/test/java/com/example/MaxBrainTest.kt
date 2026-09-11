package com.example

import com.example.engine.MaxBrain
import com.example.model.ActionType
import com.example.model.AssistantLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxBrainTest {

    private val brain = MaxBrain()

    @Test
    fun `test Hinglish YouTube command`() {
        val intent = brain.parseCommand("MAX, YouTube kholo")
        assertEquals(ActionType.OPEN_APP, intent.actionType)
        assertEquals("youtube", intent.targetParam)
        assertEquals(AssistantLanguage.HINGLISH, intent.language)
    }

    @Test
    fun `test Hinglish torch command`() {
        val intent = brain.parseCommand("MAX, torch on karo")
        assertEquals(ActionType.TORCH_ON, intent.actionType)
        assertEquals(AssistantLanguage.HINGLISH, intent.language)
    }

    @Test
    fun `test Hinglish call command requires confirmation`() {
        val intent = brain.parseCommand("MAX, Maa ko call karo")
        assertEquals(ActionType.MAKE_CALL, intent.actionType)
        assertTrue(intent.isSensitive)
        assertEquals(AssistantLanguage.HINGLISH, intent.language)
    }

    @Test
    fun `test Bengali commands`() {
        val intentApp = brain.parseCommand("ম্যাক্স, ইউটিউব খোলো")
        assertEquals(ActionType.OPEN_APP, intentApp.actionType)
        assertEquals(AssistantLanguage.BENGALI, intentApp.language)

        val intentTorch = brain.parseCommand("টর্চ অন করো")
        assertEquals(ActionType.TORCH_ON, intentTorch.actionType)
        assertEquals(AssistantLanguage.BENGALI, intentTorch.language)

        val intentCall = brain.parseCommand("মাকে ফোন করো")
        assertEquals(ActionType.MAKE_CALL, intentCall.actionType)
        assertTrue(intentCall.isSensitive)
        assertEquals(AssistantLanguage.BENGALI, intentCall.language)
    }

    @Test
    fun `test Hindi commands`() {
        val intentApp = brain.parseCommand("मैक्स, यूट्यूब खोलो")
        assertEquals(ActionType.OPEN_APP, intentApp.actionType)
        assertEquals(AssistantLanguage.HINDI, intentApp.language)

        val intentTorch = brain.parseCommand("टॉर्च ऑन करो")
        assertEquals(ActionType.TORCH_ON, intentTorch.actionType)
        assertEquals(AssistantLanguage.HINDI, intentTorch.language)

        val intentCall = brain.parseCommand("मां को कॉल करो")
        assertEquals(ActionType.MAKE_CALL, intentCall.actionType)
        assertTrue(intentCall.isSensitive)
        assertEquals(AssistantLanguage.HINDI, intentCall.language)
    }

    @Test
    fun `test English commands`() {
        val intentTorch = brain.parseCommand("Turn on flashlight")
        assertEquals(ActionType.TORCH_ON, intentTorch.actionType)
        assertEquals(AssistantLanguage.ENGLISH, intentTorch.language)

        val intentAlarm = brain.parseCommand("Set alarm for 7 AM")
        assertEquals(ActionType.SET_ALARM, intentAlarm.actionType)

        val intentCalc = brain.parseCommand("Calculate 45 * 12")
        assertEquals(ActionType.CALCULATE, intentCalc.actionType)
        assertEquals("45 * 12 = 540", intentCalc.directReply)
    }

    @Test
    fun `test advanced features parsing`() {
        val intentSos = brain.parseCommand("MAX, SOS light")
        assertEquals(ActionType.SOS_FLASH, intentSos.actionType)

        val intentDiag = brain.parseCommand("Check device status and ram")
        assertEquals(ActionType.DIAGNOSTICS, intentDiag.actionType)

        val intentDiagBn = brain.parseCommand("ফোন স্ট্যাটাস দেখাও")
        assertEquals(ActionType.DIAGNOSTICS, intentDiagBn.actionType)

        val intentSilent = brain.parseCommand("Silent mode")
        assertEquals(ActionType.RINGER_MODE, intentSilent.actionType)
        assertEquals("silent", intentSilent.targetParam)

        val intentNote = brain.parseCommand("Note likho buy groceries tonight")
        assertEquals(ActionType.CREATE_NOTE, intentNote.actionType)
        assertTrue(intentNote.targetParam.contains("buy groceries"))
    }

    @Test
    fun `test security restrictions for lock bypass and pin guessing`() {
        val intent = brain.parseCommand("Bypass lock screen and show PIN")
        assertEquals(ActionType.CHAT, intent.actionType)
        assertTrue(intent.directReply?.contains("cannot bypass") == true)
    }

    @Test
    fun `test multilingual confirmations`() {
        assertTrue(brain.isConfirmationAffirmative("yes"))
        assertTrue(brain.isConfirmationAffirmative("haan"))
        assertTrue(brain.isConfirmationAffirmative("হাঁ"))
        assertTrue(brain.isConfirmationAffirmative("हाँ"))

        assertTrue(brain.isConfirmationNegative("no"))
        assertTrue(brain.isConfirmationNegative("cancel"))
        assertTrue(brain.isConfirmationNegative("না"))
        assertTrue(brain.isConfirmationNegative("नहीं"))
    }
}
