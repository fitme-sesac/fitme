package com.example.pproject.wallet.service;

import com.example.pproject.wallet.repository.WalletCreditLotRepository;
import com.example.pproject.wallet.repository.WalletLedgerRepository;
import com.example.pproject.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    WalletRepository walletRepository;
    WalletLedgerRepository walletLedgerRepository;
    WalletCreditLotRepository walletCreditLotRepository;


}
