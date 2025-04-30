## Domaći zadatak iz predmeta Odabrana poglavlja iz projektovanja poslovnih aplikacija/Distribuirane mreze i sistemi

Zadatak: Chat aplikacija sa odredjenim funkcionalnostima

Aplikacija omogućava real-time komunikaciju korisnika putem privatnih poruka, grupnih poruka i chat soba.
**Backend** aplikacije je razvijen u Javi, koristeći **KryoNet** biblioteku za mrežnu komunikaciju preko TCP protokola. Aplikacija ne koristi bazu podataka – poruke se razmenjuju isključivo u realnom vremenu.
**Frontend** je jednostavan desktop klijent napravljen korišćenjem **Java Swing** biblioteke, koji omogućava prikaz i slanje poruka kroz grafički interfejs.

---

## Pokretanje projekta

### Server

Pokreće se sledećom komandom:

```bash
java -cp "chatServer.jar;kryonet-2.21-all.jar" rs.raf.pds.v4.z5.ChatServer 4555
```
- **chatServer.jar**: kompajlirani server kod.
- **kryonet-2.21-all.jar**: biblioteka za mrežnu komunikaciju.
- **rs.raf.pds.v4.z5.ChatServer**: puna putanja do glavne klase servera.
- **4555**: broj porta na kome server osluškuje dolazne konekcije.

### Klijent

Pokreće se sledećom komandom:

```bash
java -cp "chatClient.jar;kryonet-2.21-all.jar" rs.raf.pds.v4.z5.ChatClient localhost 4555 ImeKorisnika
```
- **chatClient.jar**: kompajlirani klijent kod.
- **kryonet-2.21-all.jar**: biblioteka za mrežnu komunikaciju.
- **rs.raf.pds.v4.z5.ChatClient**: puna putanja do glavne klase klijenta.
- **localhost**: adresa servera (ako su server i klijent na istoj mašini).
- **4555**: port na kome server prihvata konekcije.
- **ImeKorisnika**: korisničko ime koje korisnik želi da koristi.

---

## Funkcionalnosti aplikacije

* Korisnik salje privatne poruke drugim korisnicima (Server vidi sve)
    * Korisnik šalje poruku pojedinačnom korisniku:  
    `imeKorisnika: poruka`
    * Ukoliko navedeni korisnik ne postoji, posiljalac ce biti obavesten
* Multicast - korisnik istu poruku salje selektovanoj grupi korisnika (Server vidi sve)
    * Korisnik unese vise primalaca odvojenih zarezom:
    `imeKorisnika1,imeKorisnika2: poruka`
* Chat sobe - mogucnost kreiranje sobe, pozivanja drugih korisnika, ulaska u sobu, slanja poruka, odgovaranja (reply) na odredjenu poruku
    * Kreiranje sobe:  
    `CREATE ROOM imeSobe`
    * Priključivanje sobi:  
    `JOIN ROOM imeSobe`
    * Pozivanje drugih korisnika u sobu:  
    `INVITE USER imeSobe imeKorisnika`
    * Slanje poruka u sobu:  
    `ROOM MSG imeSobe poruka`
    * Prikaz liste soba korisniku koji se naknadno konektovao
    * Prikaz poslednjih 10 poruka pri ulasku u sobu
    * Preuzimanje cele istorije poruka:  
    `/getAllMessages imeSobe`
    * Odgovaranje (reply) na određenu poruku u sobi:  
    `ROOM REPLY imeSobe idPoruke poruka`
        * idPoruke - id poruke na koju se odgovara. Ispred svake poslate poruke stoji njen id

Jednostavan desktop GUI je izrađen u **Java Swing-u**.  
Omogućava sve funkcionalnosti koje su do sada implementirane:
* Prikaz prozora za chat sa listanjem poruka.
* Unos i slanje privatnih i grupnih poruka.
* Kreiranje sobe preko dugmeta.
* Prikaz dostupnih soba i pridruživanje sobi.
* Pozivanje korisnika u sobe preko dugmeta.
* Slanje poruka u sobi i prikaz poslednjih 10 poruka.
* Preuzimanje celokupne istorije sobnih poruka.
* Odgovaranje (reply) na poruke u sobi.
