package com.cognora.ecom.payment.service.dubbo;

import com.cognora.ecom.payment.api.PaymentDubboService;
import com.cognora.ecom.payment.service.IPaymentOrderService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the contract that a payment is only ever marked paid through a verified
 * channel callback.
 *
 * <p>The RPC path has no gateway in front of it and the tenant it sees comes
 * from an unsigned attachment, so an RPC method that takes a payment number and
 * a transaction id would let any caller mark a payment paid without a channel
 * proof. The assertions below fail as soon as such an entry point reappears in
 * the RPC interface, in its implementation or in the payment order service.</p>
 */
class PaymentDubboServiceContractTest {

    private static final String REMOVED_CALLBACK = "handlePaymentCallback";

    @Test
    void dubboInterfaceExposesNoUnverifiedPaymentCallback() {
        assertTrue(Arrays.stream(PaymentDubboService.class.getDeclaredMethods())
                        .noneMatch(method -> REMOVED_CALLBACK.equals(method.getName())),
                "PaymentDubboService must not declare " + REMOVED_CALLBACK);
        assertTrue(Arrays.stream(PaymentDubboService.class.getDeclaredMethods())
                        .noneMatch(PaymentDubboServiceContractTest::canMarkPaidWithoutVerification),
                "PaymentDubboService must not expose a method that can mark a payment paid "
                        + "without a verified callback");
    }

    @Test
    void dubboImplementationExposesNoUnverifiedPaymentCallback() {
        assertTrue(Arrays.stream(PaymentDubboServiceImpl.class.getDeclaredMethods())
                        .noneMatch(PaymentDubboServiceContractTest::canMarkPaidWithoutVerification),
                "PaymentDubboServiceImpl must not expose a method that can mark a payment paid "
                        + "without a verified callback");
    }

    @Test
    void paymentOrderServiceExposesOnlyTheVerifiedCallback() {
        assertTrue(Arrays.stream(IPaymentOrderService.class.getDeclaredMethods())
                        .noneMatch(method -> REMOVED_CALLBACK.equals(method.getName())),
                "IPaymentOrderService must not declare " + REMOVED_CALLBACK);
        assertTrue(Arrays.stream(IPaymentOrderService.class.getDeclaredMethods())
                        .anyMatch(method -> "handleVerifiedPaymentCallback".equals(method.getName())),
                "IPaymentOrderService must keep the verified callback entry point");
    }

    /**
     * A method that receives nothing but the payment number and the transaction
     * id of a callback can mark a payment paid. The verified path needs the
     * paid amount and the channel as well.
     */
    private static boolean canMarkPaidWithoutVerification(Method method) {
        Class<?>[] types = method.getParameterTypes();
        if (types.length != 2 || !String.class.equals(types[0]) || !String.class.equals(types[1])) {
            return false;
        }
        String name = method.getName().toLowerCase(Locale.ROOT);
        return name.contains("callback") || name.contains("paid") || name.contains("notify");
    }
}
