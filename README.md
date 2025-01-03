## Domaći zadatak iz Odabrana poglavlja iz projektovanja poslovnih aplikacija/Distribuirane mreze i sistemi

Zadatak: Chat aplikacija sa odredjenim funkcionalnostima

Commit :
* Korisnik salje privatne poruke drugim korisnicima (Server vidi sve)
    * Korisnik unese Ime korisnika kome zeli da posalje privatnu poruku + : + sadrzaj poruke  (user: poruka)
    * Ukoliko navedeni korisnik ne postoji, posiljalac ce biti obavesten
* Multicast - korisnik istu poruku salje selektovanoj grupi korisnika (Server vidi sve)
    * Korisnik unese vise primalaca odvojenih zarezom (user1, user2: poruka)
* Chat soba - mogucnost kreiranje sobe, pozivanja drugih korisnika, ulaska u sobu, slanja poruka
    * CREATE ROOM nazivSobe
    * JOIN ROOM nazivSobe
    * INVITE USER nazivSobe imeKorisnikaKojiSePoziva
    * ROOM MSG nazivSobe poruka
