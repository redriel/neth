package com.example.neth2;

import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
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
import java.security.SecureRandom;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

import static org.web3j.crypto.Hash.sha256;

/**
 * This app can:
 * - establish a connection to the Ethereum blockchain
 * - create an offline HD wallet as a JSON file
 * - send ether via transaction to a given address.
 * - call the SignUpRegistry contract
 * - show info of a given wallet, such as address and ether balance
 * - recover a lost wallet through a 12-words mnemonic phrase
 *
 * @author Gabriele Lavorato
 * @version 0.1
 */

public class MainActivity extends AppCompatActivity {

    public static final String WALLET_NAME_KEY = "WALLET_NAME_KEY";
    public static final String WALLET_DIRECTORY_KEY = "WALLET_DIRECTORY_KEY";

    //Default settings for contract gas price and gas limit
    public static final BigInteger gasPrice = DefaultGasProvider.GAS_PRICE;
    public static final BigInteger gasLimit = DefaultGasProvider.GAS_LIMIT;

    public ListView listView;
    public ArrayList<String> walletList;
    public WalletAdapter listAdapter;

    private final String password = "medium";
    private final String infuraEndpoint = "https://rinkeby.infura.io/v3/0d1f2e6517af42d3aa3f1706f96b913e";
    private final String contractAddress = "0xEcB494A8d75a64D4D18e9A659f3fA2b70Eb09324";
    private Web3j web3j;
    private File walletDirectory;
    private Button connectButton;
    private Button cloudButton;
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
        cloudButton = findViewById(R.id.btnCloud);
        cloudButton.setOnClickListener(view -> mnemonicRecovery());

        listAdapter = new WalletAdapter(MainActivity.this, R.id.walletAlias);
        listView.setAdapter(listAdapter);

        String walletPath = getFilesDir().getAbsolutePath();
        walletDirectory = new File(walletPath);
        sharedPreferences = this.getSharedPreferences("com.example.Neth2", Context.MODE_PRIVATE);
        refreshList();

    }

    private void mnemonicRecovery(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View recoveryView = getLayoutInflater().inflate(R.layout.recovery, null);
        EditText word1 = recoveryView.findViewById(R.id.word1);
        EditText word2 = recoveryView.findViewById(R.id.word2);
        EditText word3 = recoveryView.findViewById(R.id.word3);
        EditText word4 = recoveryView.findViewById(R.id.word4);
        EditText word5 = recoveryView.findViewById(R.id.word5);
        EditText word6 = recoveryView.findViewById(R.id.word6);
        EditText word7 = recoveryView.findViewById(R.id.word7);
        EditText word8 = recoveryView.findViewById(R.id.word8);
        EditText word9 = recoveryView.findViewById(R.id.word9);
        EditText word10 = recoveryView.findViewById(R.id.word10);
        EditText word11= recoveryView.findViewById(R.id.word11);
        EditText word12 = recoveryView.findViewById(R.id.word12);
        EditText array[] = {word1, word2, word3, word4, word5, word6, word7, word8, word9, word10, word11, word12};
        Button pasteButton = recoveryView.findViewById(R.id.pasteButton);
        Button recoveryButton = recoveryView.findViewById(R.id.recoveryButton);

        pasteButton.setOnClickListener(view1 -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            String sclip = clipData.toString();
            String mnemonic = sclip.substring(sclip.indexOf("T:") + 2, sclip.lastIndexOf("}") - 2);
            String[] arr = mnemonic.split(" ");
            for(int i = 0; i<12; i++){
                array[i].setText(arr[i]);
            }
        });

        recoveryButton.setOnClickListener(view1 -> {
            String mnemonic = word1.getText().toString() + " " + word2.getText().toString() + " " + word3.getText().toString() + " " +
                    word4.getText().toString() + " " + word5.getText().toString() + " " + word6.getText().toString() + " " +
                    word7.getText().toString() + " " + word8.getText().toString() + " " + word9.getText().toString() + " " +
                    word10.getText().toString() + " " + word11.getText().toString() + " " + word12.getText().toString();
            byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
            ECKeyPair privateKey = ECKeyPair.create(sha256(seed));
            System.out.println(privateKey);
            try {
                String walletName = WalletUtils.generateWalletFile(password, privateKey, walletDirectory, false);
                toastAsync("Wallet created!");
                System.out.println(walletName); //todo remove this line
                System.out.println(mnemonic); //todo remove this line
            } catch (CipherException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            refreshList();
        });

        builder.setView(recoveryView);
        builder.show();
    }

    /**
     * Setup Security provider that corrects some issues with the BouncyCastle library
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
     * Establish connection to the Rinkeby testnet via Infura endpoint
     * @param view: observed view
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
     * Create a wallet and stores it on the device
     * @param view: observed view
     */
    public void createWallet(View view) {
        try {
            String walletName = WalletUtils.generateBip39Wallet(password, walletDirectory).toString();
            sharedPreferences.edit().putString(WALLET_NAME_KEY, walletName).apply();
            sharedPreferences.edit().putString(WALLET_DIRECTORY_KEY, walletDirectory.getAbsolutePath()).apply();
            refreshList();
            toastAsync("Wallet created!");
            String mnemonic = walletName.substring(walletName.indexOf("mnemonic='")+10, walletName.indexOf("'}"));
            System.out.println(walletName); //todo remove this line
            System.out.println(mnemonic); //todo remove this line

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View recoveryView = getLayoutInflater().inflate(R.layout.mnemonic_phrase, null);
        EditText word1 = recoveryView.findViewById(R.id.word1);
        EditText word2 = recoveryView.findViewById(R.id.word2);
        EditText word3 = recoveryView.findViewById(R.id.word3);
        EditText word4 = recoveryView.findViewById(R.id.word4);
        EditText word5 = recoveryView.findViewById(R.id.word5);
        EditText word6 = recoveryView.findViewById(R.id.word6);
        EditText word7 = recoveryView.findViewById(R.id.word7);
        EditText word8 = recoveryView.findViewById(R.id.word8);
        EditText word9 = recoveryView.findViewById(R.id.word9);
        EditText word10 = recoveryView.findViewById(R.id.word10);
        EditText word11 = recoveryView.findViewById(R.id.word11);
        EditText word12 = recoveryView.findViewById(R.id.word12);
        EditText array[] = {word1, word2, word3, word4, word5, word6, word7, word8, word9, word10, word11, word12};
        Button okButton = recoveryView.findViewById(R.id.okButton);
        Button copyButton = recoveryView.findViewById(R.id.copyButton);
        String[] arr = mnemonic.split(" ");
        for(int i = 0; i<12; i++){
            array[i].setText(arr[i]);
        }
            builder.setView(recoveryView);
        final AlertDialog dialog = builder.show();
            builder.show();
        okButton.setOnClickListener(view1 -> {
            dialog.dismiss();
        });
        copyButton.setOnClickListener(view1 -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", mnemonic);
            clipboard.setPrimaryClip(clip);
            toastAsync("Address has been copied to clipboard.");
        });
        } catch (Exception e) {
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    /**
     * Send ether from a wallet to another
     * @param walletName: the wallet of the sender
     * @param address: the address of the receiver
     * @param value: the amount of ether sent
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

    /**
     * Call the SignUpRegistry contract functions
     * @param walletName: the wallet calling the contract functions
     */
    public void contractCall(String walletName) throws IOException, CipherException {
        if(connection) {
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + walletName);
            SignUpRegistry signUpRegistry = SignUpRegistry.load(contractAddress, web3j, credentials, gasPrice, gasLimit);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View transactionView = getLayoutInflater().inflate(R.layout.upload, null);
            EditText hashET = transactionView.findViewById(R.id.hashET);
            hashET.setSelection(hashET.getText().length());
            EditText eSKET = transactionView.findViewById(R.id.eSKET);
            eSKET.setSelection(eSKET.getText().length());
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
     * Delete a given wallet
     * @param walletName: the wallet to delete
     */
    public void deleteWallet(final String walletName) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Wallet")
                .setMessage("Do you want to delete this wallet from the storage?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    File file = new File(sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                            "/" + walletName);
                    if (file.exists() && file.delete()) {
                        toastAsync("Wallet deleted");
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

    /**
     * Display address and eth balance of a given wallet.
     * @param walletName: the wallet to display info
     */
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

    /**
     * Show a dialog to acquire transaction info.
     * @param walletName: the sender of the transaction
     */
    public void showTransaction(String walletName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View transactionView = getLayoutInflater().inflate(R.layout.transaction, null);
        EditText insertAddress = transactionView.findViewById(R.id.AddressET);
        EditText insertValue = transactionView.findViewById(R.id.ethBalanceET);
        Button confirmButton = transactionView.findViewById(R.id.confirmBtn);
        Button cancelButton = transactionView.findViewById(R.id.cancelBtn);
        confirmButton.setOnClickListener(view1 -> {
                sendTransaction(walletName, insertAddress.getText().toString(), insertValue.getText().toString());
        });
        cancelButton.setOnClickListener(view1 -> {
            //todo: implement close dialog
        });
        builder.setView(transactionView);
        builder.show();
    }

    /**
     * Inner class to display wallets on the device
     */
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
                    contractCall(walletTV.getText().toString());
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

    /**
     * Compute the ehter balance of a given address
     * @param address: the given address
     * @return the ether balance
     */
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
     * List all the wallets on the device
     */
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

    /**
     * Display messages and errors, mainly used for testing purposes
     */
    public void toastAsync(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

}