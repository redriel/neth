# Neth - Android Wallet Manager
Neth è una app Android per la gestione di wallet della blockchain di Ethereum.

_For english readers, go here:_ [README ENG](README_eng.md)

## Introduzione
Neth è scritta in Java e utilizza le API web3j per l'implementazione delle operazioni sulla blockchain.
Con Neth è possibile:
* stabilire una connessione alla blockchain Ethereum
* creare wallet
* effettuare transazioni tra diversi wallet
* richiamare smart contract per la gestione dei file

### Connessione ad Ethereum
Per connettersi alla blockchain Neth utilizza i servizi di endpoint forniti da Infura, che permettono la virtualizzazione dei nodi, rendendo più semplice e scalabile l'accesso alla blockchain.

### Creazione e gestione dei wallet
I wallet vengono creati come file JSON e salvati sul dispositivo. Alla creazione viene richiesta una password che verrà utilizzata per cifrare la chiave privata conservata all'interno del file JSON. Tale file viene salvato nell'internal storage del dispositivo, accessibile unicamente dalla app in runtime per eseguire le operazioni. Come precauzione aggiuntiva, ad ogni nuovo avvio della app l'utente deve inserire la password per sbloccare il wallet. In futuro sarà possibile implementare l'impronta digitale/pin/sequenza al posto della password per decifrare la chiave privata interna al JSON.

I wallet vengono creati con lo standard BIP39 e sono Hierarchical Deterministic, ossia ad ognuno di essi è associata una passphrase di 12 parole fornita alla creazione, grazie alla quale è possibile ricreare un wallet in caso di necessità.

Grazie alle API di web3j è inoltre possibile controllare in tempo reale lo stato di un wallet, ottenendo informazioni quali public address e saldo di ether. È inoltre possibile inviare ether ad un altro wallet.

### Smart Contracts
Per quanto riguarda gli smart contract, la loro implementazione è stata possibile grazie al compiler di solidity ed al Java Wrapper di web3j. Per utilizzare le funzioni di uno smart contract è necessario eseguire il deploy dello stesso e salvare il suo address, dopodichè è necessario compilare il file .sol con il compiler di solidity che crea gli oggetti .abi e .bin. Tali oggetti fungono da input per il wrapper di web3j che genera così una classe java da aggiungere alla app.
La possibilità di creare classi java autogenerate a partire dagli smart contract rende l'implementazione estremamente semplice e rapida, pur con i suoi limiti (ad esempio non è al momento possibile utilizzare funzioni dei contratti che restituiscono strutture dati).

Il contratto implementato da Neth è SignUpRegistry che permette di registrare un utente presso il contratto ed il caricamento di IPFShash di file. Al momento Neth non può interfacciarsi direttamente con IPFS (non esistono API java affidabili e le poche in circolazione sono acerbe e amatoriali) ma Infura prevede di aggiungere la virtualizzazione di nodi anche per la rete IPFS. Tale soluzione sarebbe ottimale perchè permetterebbe l'utilizzo del medesimo servizio per interfacciarsi con due sistemi diversi.
È inoltre possibile vedere gli IPFShash di tutti i documenti precedentemente caricati da un utente.

### Funzionalità
La pagina principale dell'applicazione si presenta in questo modo:

![schermata principale](https://i.imgur.com/JWUqE7m.png)

1. Mostra lo stato della connessione alla blockchain di Ethereum
2. Creazione un wallet. Una volta premuto il pulsante verrà richiesto l'inserimento di una password ed in seguito verrà mostrata la passphrase associata al wallet
3. Recupero di un wallet. Viene richiesto l'insierimento della passphrase
4. Unlock del wallet tramite password. È necessario sbloccare un wallet ogni volta che si avvia la app.
5. Mostra le informazioni del wallet: address e saldo
6. Permette di inviare ether ad un altro wallet
7. Chiamata ai contratti di registrazione utente, caricamento di hash di documenti e visione degli hash dei documenti caricati
8. Eliminazione del wallet

### Tecnologie utilizzate
 * Android Studio
 * Web3j API
 * web3j Command Line Tools
 * solc compiler
