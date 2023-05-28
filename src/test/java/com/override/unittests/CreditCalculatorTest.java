package com.override.unittests;

import com.override.unittests.enums.ClientType;
import com.override.unittests.exception.CannotBePayedException;
import com.override.unittests.exception.CentralBankNotRespondingException;
import com.override.unittests.service.CentralBankService;
import com.override.unittests.service.CreditCalculator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCalculatorTest {

    @InjectMocks
    private CreditCalculator creditCalculator;

    @Mock
    private CentralBankService centralBankService;

    @Test
    public void calculateOverpaymentGovermentTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT);
        Assertions.assertEquals(10000d, result);
    }

    @Test
    public void calculateOverpaymentBusinessTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.BUSINESS);
        Assertions.assertEquals(11000d, result);
    }

    @Test
    public void calculateOverpaymentIndividualTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.INDIVIDUAL);
        Assertions.assertEquals(12000d, result);
    }

    @Test
    public void calculateOverpaymentOnTooBigAmountTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 1000000000d;
        double monthPaymentAmount = 10000d;
        assertThrows(CannotBePayedException.class, () -> creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT));
    }

    @Test
    public void calculateOverpaymentOnManyYearCreditTest() {
        //Этот проверяет, что с кредитуемого выпиваются все соки до последней доли копейки
        when(centralBankService.getKeyRate()).thenReturn(0.01);
        double amount = 100d;
        double monthPaymentAmount = 1d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.INDIVIDUAL);
        double fractionOfResult = result % 1d;
        boolean isResultRoundedByCents = (fractionOfResult * 100 % 1) == 0;
        Assertions.assertFalse(isResultRoundedByCents);
    }

    @Test
    public void throwExceptionWhenNoConnectionTest() {
        doThrow(CentralBankNotRespondingException.class).when(centralBankService).getKeyRate();
        when(centralBankService.getDefaultCreditRate()).thenReturn(30d);
        double amount = 10000d;
        double monthPaymentAmount = 1000;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.INDIVIDUAL);
        verify(centralBankService).getDefaultCreditRate();
        assertEquals(3300.0, result);
    }


    @ParameterizedTest
    @MethodSource("methodDataProviderForCalculateOverpayment")
    public void calculateOverpaymentParameterizedTest(ClientType clientType, double expected) {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        Assertions.assertEquals(expected,
                creditCalculator.calculateOverpayment(amount, monthPaymentAmount, clientType));
    }

    static Stream<Arguments> methodDataProviderForCalculateOverpayment() {
        return Stream.of(
                Arguments.arguments(ClientType.GOVERMENT, 10000d),
                Arguments.arguments(ClientType.INDIVIDUAL, 12000d),
                Arguments.arguments(ClientType.BUSINESS, 11000d)
        );
    }
}