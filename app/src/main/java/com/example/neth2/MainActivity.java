package com.example.neth2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.io.File;
import java.security.*;
import android.widget.TextView;
import android.widget.Toast;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

public class MainActivity extends AppCompatActivity {

    private Web3j web3;
    private TextView clientName;
    private final String password = "medium";
    private String walletPath;
    private File walletDir;
    private String myPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clientName = (TextView) findViewById(R.id.clientName);
        setupBouncyCastle();
        walletPath = getFilesDir().getAbsolutePath();
        walletDir = new File(walletPath);
    }

    public void connectToEthNetwork(View view) {
        toastAsync("Connecting to Ethereum network");
        web3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/0d1f2e6517af42d3aa3f1706f96b913e"));
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if(!clientVersion.hasError()){
                toastAsync("Connected!");
                clientName.setText("Client Name:" + clientVersion.getWeb3ClientVersion());
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
            myPath = WalletUtils.generateFullNewWalletFile(password, walletDir);
            toastAsync("Wallet generated as" + walletDir);
        }
        catch (Exception e){
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    public void getAddress(View view){
        try {
            Credentials credentials = WalletUtils.loadCredentials(password, walletDir.getAbsolutePath() + "/" + myPath);
            toastAsync("Your address is " + credentials.getAddress());
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
