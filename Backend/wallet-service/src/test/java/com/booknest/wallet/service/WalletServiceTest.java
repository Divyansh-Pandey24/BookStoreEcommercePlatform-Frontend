package com.booknest.wallet.service;

import com.booknest.wallet.client.UserClient;
import com.booknest.wallet.entity.Transaction;
import com.booknest.wallet.entity.Wallet;
import com.booknest.wallet.event.WalletEventProducer;
import com.booknest.wallet.external.RazorpayService;
import com.booknest.wallet.repository.TransactionRepository;
import com.booknest.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RazorpayService razorpayService;

    @Mock
    private WalletEventProducer eventProducer;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        sampleWallet = new Wallet();
        sampleWallet.setUserId(1L);
        sampleWallet.setBalance(500.0);
    }

    @Test
    void testGetWallet_Existing() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sampleWallet));
        var result = walletService.getWallet(1L);
        assertEquals(500.0, result.getCurrentBalance());
    }

    @Test
    void testGetWallet_New() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        var result = walletService.getWallet(1L);
        assertEquals(0.0, result.getCurrentBalance());
        verify(walletRepository, times(1)).save(any());
    }

    @Test
    void testDeductMoney_Success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sampleWallet));
        
        walletService.deductMoney(1L, 100.0, 101L);
        
        assertEquals(400.0, sampleWallet.getBalance());
        verify(walletRepository, times(1)).save(sampleWallet);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testDeductMoney_InsufficientBalance() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sampleWallet));
        assertThrows(RuntimeException.class, () -> walletService.deductMoney(1L, 600.0, 101L));
    }

    @Test
    void testAddMoney_Success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sampleWallet));
        
        walletService.addMoney(1L, 200.0, 102L);
        
        assertEquals(700.0, sampleWallet.getBalance());
        verify(walletRepository, times(1)).save(sampleWallet);
        verify(transactionRepository, times(1)).save(any());
    }
}
