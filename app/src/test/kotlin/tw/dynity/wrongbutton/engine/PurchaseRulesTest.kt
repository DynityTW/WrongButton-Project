package tw.dynity.wrongbutton.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.core.monetization.PurchaseResult
import tw.dynity.wrongbutton.core.monetization.PurchaseRules

/** Google Play 購買紀錄的判斷：付款完成才開通、待付款不開通、沒確認的要確認、別的商品不算。 */
@RunWith(AndroidJUnit4::class)
class PurchaseRulesTest {
    private val PRO = tw.dynity.wrongbutton.core.monetization.ProProduct.PRODUCT_ID

    /** Play 回傳的購買 JSON：purchaseState 0＝已付款，4＝待付款。 */
    private fun purchase(product: String, state: Int, acknowledged: Boolean, token: String = "t-$product-$state") =
        Purchase(
            """{"orderId":"GPA.1","packageName":"com.wrongbutton.app","productId":"$product","purchaseTime":1,""" +
                """"purchaseState":$state,"purchaseToken":"$token","quantity":1,"acknowledged":$acknowledged}""",
            "sig",
        )

    @Test
    fun paidProIsOwnedAndUnacknowledgedOnesGetAcknowledged() {
        val paid = purchase(PRO, 0, acknowledged = false)
        assertEquals(listOf(paid), PurchaseRules.owned(listOf(paid)))
        assertEquals(listOf(paid), PurchaseRules.needsAcknowledge(listOf(paid)))
        assertEquals(PurchaseResult.Purchased, PurchaseRules.result(listOf(paid)))
        assertTrue(PurchaseRules.needsAcknowledge(listOf(purchase(PRO, 0, acknowledged = true))).isEmpty())
    }

    @Test
    fun pendingPaymentDoesNotUnlockPro() {
        val pending = purchase(PRO, 4, acknowledged = false)
        assertTrue(PurchaseRules.owned(listOf(pending)).isEmpty())
        assertTrue(PurchaseRules.pending(listOf(pending)))
        assertTrue("待付款不能確認", PurchaseRules.needsAcknowledge(listOf(pending)).isEmpty())
        assertEquals(PurchaseResult.Pending, PurchaseRules.result(listOf(pending)))
    }

    @Test
    fun otherProductsAndEmptyListsAreNotPro() {
        val other = purchase("something_else", 0, acknowledged = true)
        assertTrue(PurchaseRules.owned(listOf(other)).isEmpty())
        assertFalse(PurchaseRules.pending(listOf(other)))
        assertEquals(PurchaseResult.Failed, PurchaseRules.result(emptyList()))
    }
}
