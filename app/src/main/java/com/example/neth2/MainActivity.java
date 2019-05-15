package com.example.neth2;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import org.web3j.utils.Convert;

/**
 *
 * This app can establish a connection to the Ethereum blockchain, can create an offline wallet as a JSON file and send ether via transaction to a given address.
 */

public class MainActivity extends AppCompatActivity {

    public static final String WALLET_NAME_KEY = "WALLET_NAME_KEY";
    public static final String WALLET_DIRECTORY_KEY = "WALLET_DIRECTORY_KEY";

    private final String password = "medium";
    private Web3j web3;
    private String walletPath;
    private File walletDirectory;
    private String walletName;
    private Button connectButton;
    private SharedPreferences sharedPreferences;
    private boolean connection;

    ListView listView;
    ArrayList<String> walletList;
    WalletAdapter listAdapter;

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

    /**
     * This function create a connection to the Rinkeby testnet via Infura endpoint.
     * @param view
     */
    public void connectToEthNetwork(View view) {
        web3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/0d1f2e6517af42d3aa3f1706f96b913e"));
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if(!clientVersion.hasError()){
                toastAsync("Connected!");
                connectButton.setText("Connected to Ethereum");
                connectButton.setBackgroundColor(0xFF7CCC26);
                connection = true;
            }
            else {
                toastAsync(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }
    }

    /**
     * This function generates a JSON file wallet and stores on the phone physical storage.
     * @param view
     */
    public void createWallet(View view){
        try{
            walletName = WalletUtils.generateFullNewWalletFile(password, walletDirectory);
            sharedPreferences.edit().putString(WALLET_NAME_KEY, walletName).apply();
            sharedPreferences.edit().putString(WALLET_DIRECTORY_KEY, walletDirectory.getAbsolutePath()).apply();
            refreshList();

            toastAsync("Wallet created!");
        }
        catch (Exception e){
            toastAsync("ERROR:" + e.getMessage());
        }
    }

    /**
     * This function accesses the wallet using a password and a path. Then it sends some ether to a fixed address.
     * @param
     */
    public void sendTransaction(String walletName, String address, String value){
        try{
            Credentials credentials = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + walletName);
            TransactionReceipt receipt = Transfer.sendFunds(web3,credentials,address,
                    new BigDecimal(value), Convert.Unit.ETHER).sendAsync().get();
            toastAsync("Transaction complete: " + receipt.getTransactionHash());
            //TODO: add transaction receipt in TRANSACTION page
        }
        catch (Exception e){
            toastAsync(e.getMessage());
        }
    }

    public String getBalance(String address){
        // send asynchronous requests to get balance
        EthGetBalance ethGetBalance = null;
        try {
            ethGetBalance = web3
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
     * Setup Security provider that corrects some issues with the BuoncyCastle library.
     * @author: serso
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
     * @param message
     */
    public void toastAsync(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    public void refreshList(){
        File folder = new File(sharedPreferences.getString(WALLET_DIRECTORY_KEY, ""));
        for(File f : folder.listFiles())
        {
            if(!walletList.contains(f.getName()))
            walletList.add(f.getName());
        }
        if(listAdapter != null)
            listAdapter.notifyDataSetChanged();
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
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    public void showEthBalance(final String walletName) {
        String balance = null;
        if (connection) {
            try {
                balance = getBalance(WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                        "/" + walletName).getAddress());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CipherException e) {
                e.printStackTrace();
            }
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Ethereum Balance")
                    .setMessage("Your Ethereum balance is " + balance + " Eth")
                    .create();
            alertDialog.show();
        }
        else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Connect to the blockchain first")
                    .create();
            alertDialog.show();
        }
    }

    public void showAddress(String walletName) {
        String address = null;
        try {
            address = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                    "/" + walletName).getAddress();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CipherException e) {
            e.printStackTrace();
        }
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Wallet Address")
                .setMessage("Your wallet address is " + address)
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String address = null;
                        try {
                            address = WalletUtils.loadCredentials(password, sharedPreferences.getString(WALLET_DIRECTORY_KEY, "") +
                                    "/" + walletName).getAddress();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (CipherException e) {
                            e.printStackTrace();
                        }
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", address);
                        clipboard.setPrimaryClip(clip);
                        toastAsync("Address has been copied to clipboard.");
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    public void showTransaction(String walletName){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View transactionView = getLayoutInflater().inflate(R.layout.transaction, null);
        EditText insertAddress = (EditText) transactionView.findViewById(R.id.insertAddressET);
        EditText insertValue = (EditText) transactionView.findViewById(R.id.insertValueET);
        Button confirmButton = (Button) transactionView.findViewById(R.id.confirmBtn);
        Button cancelButton = (Button) transactionView.findViewById(R.id.cancelBtn);
        confirmButton.setOnClickListener(view1 -> {
            sendTransaction(walletName, insertAddress.getText().toString(), insertValue.getText().toString());
        });
        cancelButton.setOnClickListener(view1 -> {

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

            final TextView walletTV = (TextView) itemView.findViewById(R.id.walletAlias);
            walletTV.setText(walletList.get(position));
            final Button address = (Button) itemView.findViewById(R.id.addressButton);
            address.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAddress(walletTV.getText().toString());
                }
            });
            final Button transaction = (Button) itemView.findViewById(R.id.transactionButton);
            transaction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTransaction(walletTV.getText().toString());
                }
            });
            final Button ethBalance =  (Button) itemView.findViewById(R.id.ethBalanceButton);
            ethBalance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showEthBalance(walletTV.getText().toString());
                }
            });
            final Button deleteButton = (Button) itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteWallet(walletTV.getText().toString());
                }
            });

            return itemView;
        }

        @Override
        public String getItem(int position) {
            return walletList.get(position);
        }

    }

}
