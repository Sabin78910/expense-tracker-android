package com.sabin.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountCalculatorTest {
    @Test fun plainNumbersBehaveAsBefore() {
        assertEquals(250.0, evaluateAmount("250")!!, 0.0)
        assertEquals(12.5, evaluateAmount("12.5")!!, 0.0)
        assertEquals(0.5, evaluateAmount(".5")!!, 0.0)
    }

    @Test fun addsAndSubtracts() {
        assertEquals(195.0, evaluateAmount("120+45+30")!!, 0.0)
        assertEquals(420.0, evaluateAmount("500-80")!!, 0.0)
    }

    @Test fun multiplicationAndDivisionBindTighter() {
        assertEquals(26.0, evaluateAmount("2+3*8")!!, 0.0)
        assertEquals(94.0, evaluateAmount("100-12/2")!!, 0.0)
        assertEquals(12.0, evaluateAmount("2*3*2")!!, 0.0)
    }

    @Test fun decimalsAreSupported() {
        assertEquals(3.75, evaluateAmount("1.25*3")!!, 1e-9)
        assertEquals(0.3, evaluateAmount("0.1+0.2")!!, 1e-9)
    }

    @Test fun whitespaceIsIgnored() {
        assertEquals(195.0, evaluateAmount(" 120 + 45 + 30 ")!!, 0.0)
    }

    @Test fun unicodeOperatorsWork() {
        assertEquals(60.0, evaluateAmount("20×3")!!, 0.0)
        assertEquals(25.0, evaluateAmount("50÷2")!!, 0.0)
        assertEquals(5.0, evaluateAmount("10−5")!!, 0.0)
    }

    @Test fun invalidInputIsNull() {
        listOf("", "  ", "+", "12+", "12*", "+5", "1..2", "1.2.3", "a", "5+a", "(1+2)", "1++2", "5%", "1e5")
            .forEach { assertNull(it, evaluateAmount(it)) }
    }

    @Test fun divisionByZeroIsNull() {
        assertNull(evaluateAmount("5/0"))
        assertNull(evaluateAmount("5÷0.0"))
    }

    @Test fun zeroOrNegativeResultIsNull() {
        assertNull(evaluateAmount("5-5"))
        assertNull(evaluateAmount("5-8"))
        assertNull(evaluateAmount("0"))
    }

    @Test fun overflowBeyondLimitIsNull() {
        assertNull(evaluateAmount("999999999*10"))
        assertNull(evaluateAmount("1000000000"))
        assertEquals(999_999_999.0, evaluateAmount("999999999")!!, 0.0)
    }

    @Test fun resultIsRoundedToTwoDecimals() {
        assertEquals(3.33, evaluateAmount("10/3")!!, 0.0)
    }

    @Test fun expressionInputAllowsOperatorsButNotLetters() {
        listOf("", "12", "12+", "12+4*2", "1.5×2", "5÷2", "3-", "1 + 2").forEach { assertTrue(it, isValidAmountInput(it)) }
        listOf("a", "12x", "1e5", "(1)", "5%", "1,5").forEach { assertFalse(it, isValidAmountInput(it)) }
        assertEquals("12", filterAmountInput("12", "12x"))
        assertEquals("12+3", filterAmountInput("12", "12+3"))
    }

    @Test fun livePreviewOnlyForExpressions() {
        assertTrue(isExpression("1+2"))
        assertFalse(isExpression("250"))
        assertFalse(isExpression("12."))
    }

    @Test fun keypadAcceptsOperatorsAfterANumberOnly() {
        assertEquals("12+", keypadPress("12", '+'))
        assertEquals("12×", keypadPress("12+", '×'))
        assertEquals("", keypadPress("", '+'))
        assertEquals("12-", keypadPress("12", '-'))
    }

    @Test fun keypadLimitsApplyPerNumber() {
        assertEquals("123456789+1", keypadPress("123456789+", '1'))
        assertEquals("1+0.25", keypadPress("1+0.2", '5'))
        assertEquals("1+0.25", keypadPress("1+0.25", '5'))
        assertEquals("1+2.5", keypadPress("1+2.", '5'))
        assertEquals("1+0.", keypadPress("1+", '.'))
        assertEquals("1.5+2.", keypadPress("1.5+2", '.'))
        assertEquals("1.5+2.", keypadPress("1.5+2.", '.'))
    }
}
