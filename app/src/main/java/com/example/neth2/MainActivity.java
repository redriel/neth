package com.example.neth2;

import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * This app can establish a connection to the Ethereum blockchain, can create an offline wallet as a JSON file and send ether via transaction to a given address.
 */

public class MainActivity extends AppCompatActivity {

    public static final String WALLET_NAME_KEY = "WALLET_NAME_KEY";
    public static final String WALLET_DIRECTORY_KEY = "WALLET_DIRECTORY_KEY";
    public static final BigInteger gasPrice = DefaultGasProvider.GAS_PRICE;
    public static final BigInteger gasLimit = DefaultGasProvider.GAS_LIMIT;

    public ListView listView;
    public ArrayList<String> walletList;
    public WalletAdapter listAdapter;

    private final String password = "medium";
    private final String infuraEndpoint = "https://rinkeby.infura.io/v3/0d1f2e6517af42d3aa3f1706f96b913e";
    private final String contractAddress = "0xEcB494A8d75a64D4D18e9A659f3fA2b70Eb09324";
    private Web3j web3j;
    private String walletPath;
    private File walletDirectory;
    private String walletName;
    private Button connectButton;
    private SharedPreferences sharedPreferences;
    private boolean connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBouncyCastle();
        walletList = new ArrayList<>();
        connection = false;

        View listHeader = View.inflate(this, R.layout.activity_main_header, null);
        listView = findViewById(R.id.listView);
        listView.addHeaderView(listHeader);
        connectButton = findViewById(R.id.btnConnect);

        listAdapter = new WalletAdapter(MainActivity.this, R.id.walletAlias);
        listView.setAdapter(listAdapter);

        walletPath = getFilesDir().getAbsolutePath();
        walletDirectory = new File(walletPath);
        sharedPreferences = this.getSharedPreferences("com.example.Neth2", Context.MODE_PRIVATE);
        refreshList();

    }

    public void uploadFile(String walletName) throws IOException, CipherException {
        if(connection) {
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + walletName);
            SignUpRegistry signUpRegistry = SignUpRegistry.load(contractAddress, web3j, credentials, gasPrice, gasLimit);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View transactionView = getLayoutInflater().inflate(R.layout.upload, null);
            EditText hashET = transactionView.findViewById(R.id.hashET);
            EditText eSKET = transactionView.findViewById(R.id.eSKET);
            Button signUpButton = transactionView.findViewById(R.id.signUpBtn);
            Button uploadButton = transactionView.findViewById(R.id.uploadBtn);

            signUpButton.setOnClickListener(view1 -> {
                TransactionReceipt transactionReceipt = null;
                try {
                    transactionReceipt = signUpRegistry.addUser(credentials.getAddress(), false).sendAsync().get(2, TimeUnit.MINUTES);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                toastAsync("Successful transaction: gas used " + transactionReceipt.getGasUsed());
                System.out.println("Transaction hash: " + transactionReceipt.getTransactionHash());
                System.out.println("Successful transaction: gas used " + transactionReceipt.getGasUsed());
            });

            uploadButton.setOnClickListener(view1 -> {
                TransactionReceipt transactionReceipt = null;
                try {
                    transactionReceipt = signUpRegistry.addDocument(credentials.getAddress(),
                            hashET.getText().toString(), eSKET.getText().toString()).sendAsync().get(3, TimeUnit.MINUTES);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                toastAsync("Successful transaction: gas used " + transactionReceipt.getGasUsed());
                System.out.println("Successful transaction: gas used " + transactionReceipt.getGasUsed());
                System.out.println("Transaction hash: " + transactionReceipt.getTransactionHash());
            });

            builder.setView(transactionView);
            builder.show();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Connect to the blockchain first")
                    .create();
            alertDialog.show();
        }
    }

    /**
     * This function create a connection to the Rinkeby testnet via Infura endpoint.
     */
    public void connectToEthNetwork(View view) {
        web3j = Web3j.build(new HttpService(infuraEndpoint));
        try {
            Web3ClientVersion clientVersion = web3j.web3ClientVersion().sendAsync().get();
            if (!clientVersion.hasError()) {
                toastAsync("Connected!");
                connectButton.setText("Connected to Ethereum");
                connectButton.setBackgroundColor(0xFF7CCC26);
                connection = true;
            } else {
                toastAsync(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }
    }

    /**
     * This function generates a JSON file wallet and stores on the phone physical storage.
     */
    public void createWallet(View view) {
        try {
            walletName = WalletUtils.generateFullNewWalletFile(password, walletDirectory);
            sharedPreferences.edit().putString(WALLET_NAME_KEY, walletName).apply();
            sharedPreferences.edit().putString(WALLET_DIRECTORY_KEY, walletDirectory.getAbsolutePath()).apply();
            refreshList();

            toastAsync("Wallet created!");
        } catch (Exception e) {
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    /**
     * This function accesses the wallet using a password and a path. Then it sends some ether to a fixed address.
     */
    public void sendTransaction(String walletName, String address, String value) {
        try {
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + walletName);
            TransactionReceipt receipt = Transfer.sendFunds(web3j, credentials, address,
                    new BigDecimal(value), Convert.Unit.ETHER).sendAsync().get();
            toastAsync("Transaction complete: " + receipt.getTransactionHash());
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }
    }

    public String getBalance(String address) {
        // send asynchronous requests to get balance
        EthGetBalance ethGetBalance = null;
        try {
            ethGetBalance = web3j
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }

        BigDecimal etherValue = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
        return etherValue.toString();
    }

    /**
     * Setup Security provider that corrects some issues with the BouncyCastle library.
     */
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

    /**
     * Toast function to display messages and errors. Mainly used for testing purposes.
     */
    public void toastAsync(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    public void refreshList() {
        File folder = new File(sharedPreferences.getString(WALLET_DIRECTORY_KEY, ""));
        if (folder.exists()) {
            for (File f : folder.listFiles()) {
                if (!walletList.contains(f.getName()))
                    walletList.add(f.getName());
            }
            if (listAdapter != null)
                listAdapter.notifyDataSetChanged();
        }
    }

    public void deleteWallet(final String walletName) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Wallet")
                .setMessage("Do you want to delete this wallet from the storage?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    File file = new File(sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                            "/" + walletName);
                    if (file.exists()) {
                        file.delete();
                    } else {
                        System.err.println(
                                "I cannot find '" + file + "' ('" + file.getAbsolutePath() + "')");
                    }
                    walletList.remove(walletName);
                    listAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create();
        alertDialog.show();
    }

    public void showInfo(final String walletName)  {
        if (connection) {
            Credentials credentials = null;
            try {
                credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                        "/" + walletName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CipherException e) {
                e.printStackTrace();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View transactionView = getLayoutInflater().inflate(R.layout.info, null);
            EditText showAddress = transactionView.findViewById(R.id.AddressET);
            showAddress.setText(credentials.getAddress());
            EditText showBalance = transactionView.findViewById(R.id.ethBalanceET);
            showBalance.setText(getBalance(credentials.getAddress()));
            Button copyButton = transactionView.findViewById(R.id.copyBtn);
            copyButton.setOnClickListener(view1 -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", showAddress.getText());
                clipboard.setPrimaryClip(clip);
                toastAsync("Address has been copied to clipboard.");
            });
            builder.setView(transactionView);
            builder.show();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Connect to the blockchain first")
                    .create();
            alertDialog.show();
        }
    }

    public void showTransaction(String walletName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View transactionView = getLayoutInflater().inflate(R.layout.transaction, null);
        EditText insertAddress = transactionView.findViewById(R.id.AddressET);
        EditText insertValue = transactionView.findViewById(R.id.ethBalanceET);
        Button confirmButton = transactionView.findViewById(R.id.confirmBtn);
        Button cancelButton = transactionView.findViewById(R.id.cancelBtn);
        confirmButton.setOnClickListener(view1 -> sendTransaction(walletName, insertAddress.getText().toString(), insertValue.getText().toString()));
        cancelButton.setOnClickListener(view1 -> {
            //todo: implement close dialog
        });
        builder.setView(transactionView);
        builder.show();
    }

    public class WalletAdapter extends ArrayAdapter<String> {

        public WalletAdapter(Context context, int textView) {
            super(context, textView);
        }

        @Override
        public int getCount() {
            return walletList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.list_items, parent, false);

            final TextView walletTV = itemView.findViewById(R.id.walletAlias);
            walletTV.setText(walletList.get(position));

            final Button address = itemView.findViewById(R.id.infoButton);
            address.setOnClickListener(view -> showInfo(walletTV.getText().toString()));

            final Button transaction = itemView.findViewById(R.id.transactionButton);
            transaction.setOnClickListener(view -> showTransaction(walletTV.getText().toString()));

            final Button uploadDocument = itemView.findViewById(R.id.uploadDocumentButton);
            uploadDocument.setOnClickListener(view -> {
                try {
                    uploadFile(walletTV.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            final Button deleteButton = itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(view -> deleteWallet(walletTV.getText().toString()));

            return itemView;
        }

        @Override
        public String getItem(int position) {
            return walletList.get(position);
        }

    }

}