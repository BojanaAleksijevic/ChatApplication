## Domaći zadatak iz predmeta Odabrana poglavlja iz projektovanja poslovnih aplikacija/Distribuirane mreze i sistemi

Zadatak: Chat aplikacija sa odredjenim funkcionalnostima

Funkcionalnosti aplikacije :
* Korisnik salje privatne poruke drugim korisnicima (Server vidi sve)
    * Korisnik unese Ime korisnika kome zeli da posalje privatnu poruku + : + sadrzaj poruke  (user: poruka)
    * Ukoliko navedeni korisnik ne postoji, posiljalac ce biti obavesten
* Multicast - korisnik istu poruku salje selektovanoj grupi korisnika (Server vidi sve)
    * Korisnik unese vise primalaca odvojenih zarezom (user1, user2: poruka)
* Chat soba - mogucnost kreiranje sobe, pozivanja drugih korisnika, ulaska u sobu, slanja poruka, odgovaranja (reply) na odredjenu poruku
    * CREATE ROOM imeSobe
    * JOIN ROOM imeSobe
    * INVITE USER imeSobe imeKorisnikaKojiSePoziva
    * ROOM MSG imeSobe poruka
    * Prikaz liste soba korisniku koji se naknadno konektovao
    * Prikaz poslednjih 10 poruka iz sobe korisniku koji se naknadno prikljucio toj sobi
    * Korisnik salje zahtev za prikaz svih poruka iz sobe u koju se prikljucio komandom: /getAllMessages imeSobe
    * Korisnik moze da izvrsi reply na poruku komandom: ROOM REPLY imeSobe idPoruke poruka
        * idPoruke - id poruke na koju se odgovara. Ispred svake poslate poruke stoji njen id

Napravljen je jednostavan desktop GUI korišćenjem Java Swing biblioteke. Implementirano je do sad:
* Prozor za prikaz chat poruka za unosa i slanja obicnih poruka (privatnih i grupnih tj. multicast)
* Dugme za kreiranje sobe
* Dugme za prikljucivanje nekoj sobi, koje izbacuje opcije dostupnih soba
* Dugme za poziv drugih korisnika u odredjenu sobu
* Slanje poruka u sobi i prikaz poslednjih 10 poruka kada se neko naknadno prikljuci
