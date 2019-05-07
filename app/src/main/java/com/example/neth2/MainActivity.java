package com.example.neth2;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.io.File;
import java.math.BigDecimal;
import java.security.*;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

public class MainActivity extends AppCompatActivity {


    public static final String WALLET_NAME_KEY = "WALLET_NAME_KEY";
    public static final String WALLET_DIRECTORY_KEY = "WALLET_DIRECTORY_KEY";
    private Web3j web3;
    private final String password = "medium";
    private String walletPath;
    private File walletDirectory;
    private String walletName;
    private Button connectButton;
    private Button createWalletButton;
    private TextView transactionHashTv;
    private TextView addressTv;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = (Button) findViewById(R.id.btnConnect);
        createWalletButton = (Button) findViewById(R.id.btnCreateWallet);
        transactionHashTv = (TextView) findViewById(R.id.tvTransactionHash);
        addressTv = (TextView) findViewById(R.id.tvAddress);
        setupBouncyCastle();
        walletPath = getFilesDir().getAbsolutePath();
        walletDirectory = new File(walletPath);
        sharedPreferences = this.getSharedPreferences("com.example.Neth2", Context.MODE_PRIVATE);
        String storedPath = sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                "/" + sharedPreferences.getString(WALLET_NAME_KEY, "");

        if(storedPath.contains("UTC")){
            createWalletButton.setText("Wallet created");
            //createWalletButton.setEnabled(false);
            createWalletButton.setBackgroundColor(0xFF7CCC26);
        }
    }

    public void connectToEthNetwork(View view) {
        web3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/0d1f2e6517af42d3aa3f1706f96b913e"));
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if(!clientVersion.hasError()){
                toastAsync("Connected!");
                connectButton.setText("Connected to Ethereum");
                connectButton.setBackgroundColor(0xFF7CCC26);
            }
            else {
                toastAsync(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }
    }

    public void createWallet(View view){
        try{
            walletName = WalletUtils.generateFullNewWalletFile(password, walletDirectory);
            sharedPreferences.edit().putString(WALLET_NAME_KEY, walletName).apply();
            sharedPreferences.edit().putString(WALLET_DIRECTORY_KEY, walletDirectory.getAbsolutePath()).apply();
            
            createWalletButton.setText("Wallet created");
            createWalletButton.setBackgroundColor(0xFF7CCC26);
            //createWalletButton.setEnabled(false);
            toastAsync("Wallet created!");
        }
        catch (Exception e){
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    public void getAddress(View view){
        try{
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + sharedPreferences.getString(WALLET_NAME_KEY, ""));
            addressTv.setText(credentials.getAddress());
        }
        catch (Exception e){
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    public void toastAsync(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    public void sendTransaction(View v){
        try{
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + sharedPreferences.getString(WALLET_NAME_KEY, ""));
            TransactionReceipt receipt = Transfer.sendFunds(web3,credentials,"0x31B98D14007bDEe637298086988A0bBd31184523",new BigDecimal("0.0001"), Convert.Unit.ETHER).sendAsync().get();
            toastAsync("Transaction complete: " + receipt.getTransactionHash());
            transactionHashTv.setText(receipt.getTransactionHash());
        }
        catch (Exception e){
            toastAsync(e.getMessage());
        }
    }

    // Setup Security provider by serso
    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

}
