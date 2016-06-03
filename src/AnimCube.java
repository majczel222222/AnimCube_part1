import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.Hashtable;

public final class AnimCube extends Applet implements Runnable, MouseListener, MouseMotionListener {


    // ZEWNETRZNA KONFIGURACJA KOSTKI
    private final Hashtable konfiguracja = new Hashtable();

    // BARWY, WEKTORY I NUMERACJA NAROZNIKOW I SCIAN
    private Color kolorTla;
    private Color przyciskKoloruTla;
    private final Color[] barwy = new Color[24];
    private final int[][] kostka = new int[6][9];
    private final int[][] stanPoczatkowyKostki = new int[6][9];
    private static final double[][] wektoryNormalneScianKostki = { {0.0, -1.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, -1.0}, {0.0, 0.0, 1.0}, {-1.0, 0.0, 0.0}, {1.0, 0.0, 0.0} };
    private static final double[][] wspolrzedneNaroznikowKostki = { {-1.0, -1.0, -1.0}, {1.0, -1.0, -1.0}, {1.0, -1.0, 1.0}, {-1.0, -1.0, 1.0}, {-1.0, 1.0, -1.0}, {1.0, 1.0, -1.0}, {1.0, 1.0, 1.0}, {-1.0, 1.0, 1.0} };
    private static final int[][] naroznikiScianKostki = { {0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2} };
    private static final int[][] naroznikiScianPrzeciwnych = { {0, 3, 2, 1}, {0, 3, 2, 1}, {3, 2, 1, 0}, {3, 2, 1, 0}, {0, 3, 2, 1}, {0, 3, 2, 1} };
    private static final int[][] scianyPrzylegle = { {2, 5, 3, 4}, {4, 3, 5, 2}, {4, 1, 5, 0}, {5, 1, 4, 0}, {0, 3, 1, 2}, {2, 1, 3, 0} };
    private int warstwaObracana;
    private int obracanie;
    private static final int[] kierunkiScianPrzyObracaniu = {1, 1, -1, -1, -1, -1};

    // URZADZAM DANE: OBSERWATOR I WIDOKI
    private final double[] obserwator = {0.0, 0.0, -1.0};
    private final double[] widokBokiem = {1.0, 0.0, 0.0};
    private final double[] widokPionowy = new double[3];
    private final double[] wstepnyObserwator = new double[3];
    private final double[] wstepnyWidokBokiem = new double[3];
    private final double[] wstepnyWidokPionowy = new double[3];

    private double katPoObrocie;
    private double katPrzedObrotem;
    private boolean stanObojetny = true;
    private boolean stanGdzieMoznaObracacWarstwami;
    private boolean dokonywanieLustrzanegoOdbicia;
    private boolean dokonywanieZmianNaKostce;
    private boolean dokonywanieObrotuWarstwy;
    private boolean przeciaganie;
    private int ustawienieKostki;
    private int torRuchu;
    private int wysokoscPrzyciskow;
    private int zmianaWysokosci = 6;

    public void init() {

        // OTRZYMYWANIE DANYCH Z MYSZY
        addMouseListener(this);
        addMouseMotionListener(this);

        // USTAWIAM BARWY
        barwy[0] = new Color(255, 84, 0);
        barwy[1] = new Color(255, 0, 0);
        barwy[2] = new Color(0, 255, 0);
        barwy[3] = new Color(0, 0, 255);
        barwy[4] = new Color(255, 255, 255);
        barwy[5] = new Color(255, 255, 0);

        // USTAWIAM DOMYSLNA KONFIGURACJE
        String wspolczynnikKonfiguracji = getParameter("konfiguracja");
        kolorTla = Color.DARK_GRAY;
        przyciskKoloruTla = kolorTla;

        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 9; j++) {
                kostka[i][j] = i;
            }

        // USTAWIAM SKEWENCJE RUCHU
        wspolczynnikKonfiguracji = getParameter("ruch");
        torRuchu = 0;
        wektorNormalny(mnozenieWektorow(widokPionowy, obserwator, widokBokiem));

        // POZWOLENIE NA OBRACANIE WARSTW
        if ("0".equals(wspolczynnikKonfiguracji))
            dokonywanieZmianNaKostce = false;
        else
            dokonywanieZmianNaKostce = true;

        ustawienieKostki = 1;

        wspolczynnikKonfiguracji = getParameter("ustawienieKostki");
        if (wspolczynnikKonfiguracji != null) {
            if ("0".equals(wspolczynnikKonfiguracji))
                ustawienieKostki = 0;
            else if ("1".equals(wspolczynnikKonfiguracji))
                ustawienieKostki = 1;
            else if ("2".equals(wspolczynnikKonfiguracji))
                ustawienieKostki = 2;
        }

        // USTAWIAM WARTOSCI POCZATKOWE
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 9; j++)
                stanPoczatkowyKostki[i][j] = kostka[i][j];

        for (int i = 0; i < 3; i++) {
            wstepnyObserwator[i] = obserwator[i];
            wstepnyWidokBokiem[i] = widokBokiem[i];
            wstepnyWidokPionowy[i] = widokPionowy[i];
        }
    }

    public String getParameter(String name) {
        String stala = super.getParameter(name);
        if (stala == null)
            return (String) konfiguracja.get(name);

        return stala;
    }

    private static int dlugoscRuchuTeraz(int[] przesuwanie) {
        int length = 0;
        for (int i = 0; i < przesuwanie.length; i++)
            if (przesuwanie[i] < 1000)
                length++;
        return length;
    }

    private static int torRuchuTeraz(int[] przesuwanie, int stan) {
        int pozyc = 0;
        for (int i = 0; i < stan; i++)
            if (przesuwanie[i] < 1000)
                pozyc++;
        return pozyc;
    }

    private void clear() {
        torRuchu = 0;
        dokonywanieLustrzanegoOdbicia = false;
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 9; j++)
                kostka[i][j] = stanPoczatkowyKostki[i][j];
        for (int i = 0; i < 3; i++) {
            obserwator[i] = wstepnyObserwator[i];
            widokBokiem[i] = wstepnyWidokBokiem[i];
            widokPionowy[i] = wstepnyWidokPionowy[i];
        }

    }

    // WYMIARY KLOCKOW
    private final int[][][] klockiGorne = new int[6][][];
    private final int[][][] klockiSrodkowe = new int[6][][];
    private final int[][][] klockiDolne = new int[6][][];
    private static final int[][][] klockiKostkiNieruchomej = { {{0, 3}, {0, 3}}, {{0, 3}, {0, 3}}, {{0, 3}, {0, 3}}, {{0, 3}, {0, 3}}, {{0, 3}, {0, 3}}, {{0, 3}, {0, 3}} };

    // DANE O KIERUNKACH I WYMIARACH KLOCKOW
    private static final int[][][] daneKlockowGornych = { {{0, 0}, {0, 0}}, {{0, 3}, {0, 3}}, {{0, 3}, {0, 1}}, {{0, 1}, {0, 3}}, {{0, 3}, {2, 3}}, {{2, 3}, {0, 3}} };
    private static final int[][][] daneKlockowSrodkowych = { {{0, 0}, {0, 0}}, {{0, 3}, {1, 2}}, {{1, 2}, {0, 3}} };
    private static final int[][] wymiaryKlockowGornych = { {1, 0, 3, 3, 2, 3}, {0, 1, 5, 5, 4, 5}, {2, 3, 1, 0, 3, 2}, {4, 5, 0, 1, 5, 4}, {3, 2, 2, 4, 1, 0}, {5, 4, 4, 2, 0, 1} };
    private static final int[][] wymiaryKlockowSrodkowych = { {0, 0, 2, 2, 1, 2}, {0, 0, 2, 2, 1, 2}, {1, 2, 0, 0, 2, 1}, {1, 2, 0, 0, 2, 1}, {2, 1, 1, 1, 0, 0}, {2, 1, 1, 1, 0, 0} };
    private static final int[][] wymiaryKlockowDolnych = { {0, 1, 5, 5, 4, 5}, {1, 0, 3, 3, 2, 3}, {4, 5, 0, 1, 5, 4}, {2, 3, 1, 0, 3, 2}, {5, 4, 4, 2, 0, 1}, {3, 2, 2, 4, 1, 0} };


    // PROGRAM TWORZY DLA SZESCIANU WARSTWY
    private void warstwyKostki(int warstwa) {
        for (int i = 0; i < 6; i++) {
            klockiGorne[i] = daneKlockowGornych[wymiaryKlockowGornych[warstwa][i]];
            klockiDolne[i] = daneKlockowGornych[wymiaryKlockowDolnych[warstwa][i]];
            klockiSrodkowe[i] = daneKlockowSrodkowych[wymiaryKlockowSrodkowych[warstwa][i]];
        }
        stanObojetny = false;
    }

    private void obracanieWarstw(int[][] kostka, int warstwa, int liczbaPorzadkowa, int stan) {
        switch (stan) {
            case 3:
                obracanieWarstwy(kostka, warstwa ^ 1, liczbaPorzadkowa, false);
            case 2:
                obracanieWarstwy(kostka, warstwa, 4 - liczbaPorzadkowa, false);
            case 1:
                obracanieWarstwy(kostka, warstwa, 4 - liczbaPorzadkowa, true);
                break;
            case 5:
                obracanieWarstwy(kostka, warstwa ^ 1, 4 - liczbaPorzadkowa, false);
                obracanieWarstwy(kostka, warstwa, 4 - liczbaPorzadkowa, false);
                break;
            case 4:
                obracanieWarstwy(kostka, warstwa ^ 1, liczbaPorzadkowa, false);
            default:
                obracanieWarstwy(kostka, warstwa, 4 - liczbaPorzadkowa, false);
        }
    }

    // NUMEROWANIE WARSTW
    private static final int[] kolejnoscOkresu = {0, 1, 2, 5, 8, 7, 6, 3};
    private static final int[] stopnieOkresu = {1, 3, -1, -3, 1, 3, -1, -3};
    private static final int[] rozstawienieOkresu = {0, 2, 8, 6, 3, 1, 5, 7};
    private static final int[][] warstwyBoczneOkresu = { {3, 3, 3, 0}, {2, 1, 1, 1}, {3, 3, 0, 0}, {2, 1, 1, 2}, {3, 2, 0, 0}, {2, 2, 0, 1} };
    private static final int[][] srodekOkresu = { {7, 7, 7, 4}, {6, 5, 5, 5}, {7, 7, 4, 4}, {6, 5, 5, 6}, {7, 6, 4, 4}, {6, 6, 4, 5} };

    private final int[] zmianaBuforu = new int[12];

    /*************************************OBRACANIE WARSTYW***********************************************************/
    private void obracanieWarstwy(int[][] kostka, int warstwa, int liczbaPorzadkowa, boolean srodek) {
        if (!srodek) {
            // OBRACANIE GORNYCH KOSTEK
            for (int i = 0; i < 8; i++)
                zmianaBuforu[(i + liczbaPorzadkowa * 2) % 8] = kostka[warstwa][kolejnoscOkresu[i]];
            for (int i = 0; i < 8; i++)
                kostka[warstwa][kolejnoscOkresu[i]] = zmianaBuforu[i];
        }

        // OBRCANIE BOCZNYCH KOSTEK
        int z = liczbaPorzadkowa * 3;
        for (int i = 0; i < 4; i++) {
            int x = scianyPrzylegle[warstwa][i];
            int y = srodek ? srodekOkresu[warstwa][i] : warstwyBoczneOkresu[warstwa][i];
            int stopien = stopnieOkresu[y];
            int rozstaw = rozstawienieOkresu[y];
            for (int j = 0; j < 3; j++) {
                zmianaBuforu[z % 12] = kostka[x][j * stopien + rozstaw];
                z++;
            }
        }

        z = 0;
        for (int i = 0; i < 4; i++) {
            int x = scianyPrzylegle[warstwa][i];
            int y = srodek ? srodekOkresu[warstwa][i] : warstwyBoczneOkresu[warstwa][i];
            int stopien = stopnieOkresu[y];
            int rozstaw = rozstawienieOkresu[y];

            for (int j = 0; j < 3; j++) {
                kostka[x][j * stopien + rozstaw] = zmianaBuforu[z];
                z++;
            }
        }
    }


    /*******************************************WEKTORY, DANE MYSZY I TABLICE******************************************/

    // GRAFIKA I OBRAZ
    private Graphics tekstury = null;
    private Image obraz = null;

    // WYMIARY GRAFIKI
    private int szerokosc;
    private int wysokosc;

    // POBIERANIE POZYCJI MYSZY PRZED OBRACANIEM WARSTWY
    private int wczesniejszaWspolrzednaX;
    private int wczesniejszaWspolrzednaY;

    // POBIERANIE POZYCJI MYSZY ABY PROGRAM MOGL ZADECYDOWAC CZY OBROCIC KOSTKE CZY WARSTWE
    private int wcześniejszePolozenieMyszyNaX;
    private int wcześniejszePolozenieMyszyNaY;

    // WEKTORY I ZMIENNE ZWIAZANIE Z PRZECIAGANIEM
    private int obszarPrzeciaganiaMysza;
    private final int[][] przeciaganieNaroznikowNaX = new int[18][4];
    private final int[][] przeciaganieNaroznikowNaY = new int[18][4];
    private final double[] kierunekPrzeciaganiaNaX = new double[18];
    private final double[] kierunekPrzeciaganiaNaY = new double[18];
    private static final int[][][] przeciaganieKlockow = { {{0, 0}, {3, 0}, {3, 1}, {0, 1}}, {{3, 0}, {3, 3}, {2, 3}, {2, 0}}, {{3, 3}, {0, 3}, {0, 2}, {3, 2}}, {{0, 3}, {0, 0}, {1, 0}, {1, 3}}, {{0, 1}, {3, 1}, {3, 2}, {0, 2}}, {{2, 0}, {2, 3}, {1, 3}, {1, 0}} };
    private static final int[][] kierunkiPola = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 0}, {0, 1}};
    private static final int[][] kierunkiObrotu = { {1, 1, 1, 1, 1, -1}, {1, 1, 1, 1, 1, -1}, {1, -1, 1, -1, 1, 1}, {1, -1, 1, -1, -1, 1}, {-1, 1, -1, 1, -1, -1}, {1, -1, 1, -1, 1, 1}  };
    private int[] przeciaganeWarstwy = new int[18];
    private int[] trybyPrzeciaganychWarstw = new int[18];
    private double przeciaganiePoX;
    private double przeciaganiePoY;

    // TABLICE ZNAKOW KIERUNKOW OBROTOW
    private static final int[][][] odwracanieCosinusa = { {{1, 0, 0}, {0, 0, 0}, {0, 0, 1}}, {{1, 0, 0}, {0, 1, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 1, 0}, {0, 0, 1}}  };
    private static final int[][][] odwracanieSinusa = { {{0, 0, 1}, {0, 0, 0}, {-1, 0, 0}}, {{0, 1, 0}, {-1, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 1}, {0, -1, 0}} };
    private static final int[][][] odwracanieWektora = { {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}, {0, 0, 1}}, {{1, 0, 0}, {0, 0, 0}, {0, 0, 0}} };
    private static final int[] odwracanieZnaku = {1, -1, 1, -1, 1, -1};

    // WEKTORY WIDOKOW DLA OBROTU WARSTWY I KOSTKI
    private double[] tymczasowyObserwator = new double[3];
    private double[] tymczasowyWidokNaX = new double[3];
    private double[] tymczasowyWidokNaY = new double[3];
    private double[] tymczasowyDrugiObserwator = new double[3];
    private double[] tymczasowyDrugiWidokNaX = new double[3];
    private final double[] tymczasowyDrugiWidokNaY = new double[3];

    // WEKTORY DO USTALENIA PUNKTOW WIDZENIA
    private double[] punktWidzenia1 = new double[3];
    private double[] punktWidzenia2 = new double[3];
    private double[] widokNormalny = new double[3];

    // ROZNE USTAWIENIA WIDOKOW
    private double[][] ustawienieObserwatora = new double[3][];
    private double[][] ustawienieWidokuNaX = new double[3][];
    private double[][] ustawienieWidokuNaY = new double[3][];
    private int[][] kolejnoscObserwatorow = {{1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 2}};
    private int[][][][] ustawienieKlockow = new int[3][][][];
    private int[][] trybKlockow = {{0, 2, 2}, {2, 1, 2}, {2, 2, 2}, {2, 2, 2}, {2, 2, 2}, {2, 2, 2}};
    private int[][] tworzenieUporzadkowania = {{0, 1, 2}, {2, 1, 0}, {0, 2, 1}};


    /******************************************RYSOWANIE***************************************************************/
    public void paint(Graphics g) {
        Dimension rozmiar = getSize();

        if (obraz == null || rozmiar.width != szerokosc || rozmiar.height - wysokoscPrzyciskow != wysokosc) {

            // WYMIARY GRAFIKI
            szerokosc = 1920;
            wysokosc = 1080;
            obraz = createImage(szerokosc, wysokosc);
            tekstury = obraz.getGraphics();
        }

        // KOLORY I TLO GRAFIKI
        tekstury.setColor(kolorTla);
        tekstury.setClip(0, 0, szerokosc, wysokosc);
        tekstury.fillRect(0, 0, szerokosc, wysokosc);

        obszarPrzeciaganiaMysza = 0;

        // JESLI KOSTKA JEST NIERUCHOMA
        if (stanObojetny)
            ustalanieKlocka(obserwator, widokBokiem, widokPionowy, klockiKostkiNieruchomej, 3);

        else {

            // JESLI KOSTKA MA OBRACANE WARSTWY
            double cosA = Math.cos(katPrzedObrotem + katPoObrocie);
            double sinA = Math.sin(katPrzedObrotem + katPoObrocie) * odwracanieZnaku[warstwaObracana];
            int i = 0;

            // USTAWIAM OBSERWATORA DLA OBRACANIA
            while (i < 3) {
                tymczasowyObserwator[i] = 0;
                tymczasowyWidokNaX[i] = 0;
                for (int j = 0; j < 3; j++) {
                    int axis = warstwaObracana / 2;
                    tymczasowyObserwator[i] += obserwator[j] * (odwracanieWektora[axis][i][j] + odwracanieCosinusa[axis][i][j] * cosA + odwracanieSinusa[axis][i][j] * sinA);
                    tymczasowyWidokNaX[i] += widokBokiem[j] * (odwracanieWektora[axis][i][j] + odwracanieCosinusa[axis][i][j] * cosA + odwracanieSinusa[axis][i][j] * sinA);
                }
                i++;
            }

            mnozenieWektorow(tymczasowyWidokNaY, tymczasowyObserwator, tymczasowyWidokNaX);

            // USTAWIAM OBSERWATORA Z DRUGIEJ STRONY
            double cosB = Math.cos(katPrzedObrotem - katPoObrocie);
            double sinB = Math.sin(katPrzedObrotem - katPoObrocie) * odwracanieZnaku[warstwaObracana];
            for (i = 0; i < 3; i++) {
                tymczasowyDrugiObserwator[i] = 0;
                tymczasowyDrugiWidokNaX[i] = 0;
                for (int j = 0; j < 3; j++) {
                    int axis = warstwaObracana / 2;
                    tymczasowyDrugiObserwator[i] += obserwator[j] * (odwracanieWektora[axis][i][j] + odwracanieCosinusa[axis][i][j] * cosB + odwracanieSinusa[axis][i][j] * sinB);
                    tymczasowyDrugiWidokNaX[i] += widokBokiem[j] * (odwracanieWektora[axis][i][j] + odwracanieCosinusa[axis][i][j] * cosB + odwracanieSinusa[axis][i][j] * sinB);
                }
            }

            mnozenieWektorow(tymczasowyDrugiWidokNaY, tymczasowyDrugiObserwator, tymczasowyDrugiWidokNaX);

            ustawienieObserwatora[0] = obserwator;
            ustawienieWidokuNaX[0] = widokBokiem;
            ustawienieWidokuNaY[0] = widokPionowy;
            ustawienieObserwatora[1] = tymczasowyObserwator;
            ustawienieWidokuNaX[1] = tymczasowyWidokNaX;
            ustawienieWidokuNaY[1] = tymczasowyWidokNaY;
            ustawienieObserwatora[2] = tymczasowyDrugiObserwator;
            ustawienieWidokuNaX[2] = tymczasowyDrugiWidokNaX;
            ustawienieWidokuNaY[2] = tymczasowyDrugiWidokNaY;
            ustawienieKlockow[0] = klockiGorne;
            ustawienieKlockow[1] = klockiSrodkowe;
            ustawienieKlockow[2] = klockiDolne;


            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia1, obserwator), 5.0), skalowanieWektorow(kopiowanieWektorow(widokNormalny, wektoryNormalneScianKostki[warstwaObracana]), 1.0 / 3.0));
            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia2, obserwator), 5.0), skalowanieWektorow(kopiowanieWektorow(widokNormalny, wektoryNormalneScianKostki[warstwaObracana ^ 1]), 1.0 / 3.0));

            double tworzenieGornych = tworzenieWektorow(punktWidzenia1, wektoryNormalneScianKostki[warstwaObracana]);
            double tworzenieDolnych = tworzenieWektorow(punktWidzenia2, wektoryNormalneScianKostki[warstwaObracana ^ 1]);
            int trybPorzadkowania;

            // GORNA WARSTWA JEST NIEWIDOCZNA
            if (tworzenieGornych < 0 && tworzenieDolnych > 0)
                trybPorzadkowania = 0;

            // DOLNA WARSTWA JEST NIEWIDOCZNA
            else if (tworzenieGornych > 0 && tworzenieDolnych < 0)
                trybPorzadkowania = 1;

            // GORNA I DOLNA WARSTWA  SA NIEWIDOCZNE
            else
                trybPorzadkowania = 2;

            ustalanieKlocka(ustawienieObserwatora [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [0]]],
                    ustawienieWidokuNaX [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [0]]],
                    ustawienieWidokuNaY [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [0]]],
                    ustawienieKlockow [tworzenieUporzadkowania [trybPorzadkowania] [0]],
                    trybKlockow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [0]]);

            ustalanieKlocka(ustawienieObserwatora [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [1]]],
                    ustawienieWidokuNaX [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [1]]],
                    ustawienieWidokuNaY [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [1]]],
                    ustawienieKlockow [tworzenieUporzadkowania [trybPorzadkowania] [1]],
                    trybKlockow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [1]]);

            ustalanieKlocka(ustawienieObserwatora [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [2]]],
                    ustawienieWidokuNaX [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [2]]],
                    ustawienieWidokuNaY [kolejnoscObserwatorow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [2]]],
                    ustawienieKlockow [tworzenieUporzadkowania [trybPorzadkowania] [2]],
                    trybKlockow [obracanie] [tworzenieUporzadkowania [trybPorzadkowania] [2]]);
        }
        g.drawImage(obraz, 0, 0, this);
    }

    // METODA update() WEZWANA W ODPOWIEDZI NA repaint() I W CELU SAMEGO RYSOWANIA
    public void update(Graphics g) {
        paint(g);
    }

    // WSPOLRZEDNE WIELOKATA, KTORY BEDZIE POKOLOROWANY POZNIEJ
    private final int[] wypelnijNaX = new int[4];
    private final int[] wypelnijNaY = new int[4];

    // OKRESLAM WSPOLRZEDNE WIDOCZNYCH NAROZNIKOW
    private final double[] wspolrzedneX = new double[8];
    private final double[] wspolrzedneY = new double[8];
    private final double[][] koordynatyXowe = new double[6][4];
    private final double[][] koordynatyYkowe = new double[6][4];
    private static final double[][] granica = {{0.02, 0.02}, {0.98, 0.02}, {0.98, 0.98}, {0.02, 0.98}};
    private static final int[][] stopnie = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

    /*********************RYSOWANIE KRAWEDZI, MIEDZY-SCIAN, KOSTKI I KLOCKOW NA EKRANIE*******************************/
    private void ustalanieKlocka(double[] eye, double[] eyeX, double[] eyeY, int[][][] klocki, int tryb) {

        // ILUZJA OPTYCZNA
        // 3D PRZEDSTAWIONA NA PLASZCZYZNIE 2D
        for (int i = 0; i < 8; i++) {
            double wartoscMinimalna = szerokosc < wysokosc ? szerokosc : wysokosc - zmianaWysokosci;
            double wspX = wartoscMinimalna / 3.7 * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eyeX);
            double wspY = wartoscMinimalna / 3.7 * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eyeY);
            double wspZ = wartoscMinimalna / (5.0) * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eye);
            wspX = wspX / (1 - wspZ / wartoscMinimalna);
            wspY = wspY / (1 - wspZ / wartoscMinimalna);
            wspolrzedneX[i] = szerokosc / 2.0 + wspX;
            if (ustawienieKostki == 0)
                wspolrzedneY[i] = (wysokosc - zmianaWysokosci) / 2.0 * -wspY;
            else if (ustawienieKostki == 2)
                wspolrzedneY[i] = wysokosc - zmianaWysokosci - (wysokosc - zmianaWysokosci) / 2.0 * -wspY;
            else
                wspolrzedneY[i] = (wysokosc - zmianaWysokosci) / 2.0 - wspY;
        }


        // DLA SZESCIU SCIAN POBIERZ DANE NAROZNIKOW
        for (int i = 0; i < 6; i++) {

            // DANE NAROZNIKOW - POBIERANIE WSPOLRZEDNYCH
            for (int j = 0; j < 4; j++) {
                koordynatyXowe[i][j] = wspolrzedneX[naroznikiScianKostki[i][j]];
                koordynatyYkowe[i][j] = wspolrzedneY[naroznikiScianKostki[i][j]];
            }
        }

        // RYSOWANIE KONTUROW(KRAWEDZI)
        for (int i = 0; i < 6; i++) { // all faces
            int bocznaSzerokosc = klocki[i][0][1] - klocki[i][0][0];
            int bocznaWysokosc = klocki[i][1][1] - klocki[i][1][0];
            if (bocznaSzerokosc > 0 && bocznaWysokosc > 0) {

                // DANE NAROZNIKOW DO RYSOWANIA KRAWEDZI
                for (int j = 0; j < 4; j++)
                    pobierzNarozniki(i, j, wypelnijNaX, wypelnijNaY, klocki[i][0][stopnie[j][0]], klocki[i][1][stopnie[j][1]], dokonywanieLustrzanegoOdbicia);
                if (bocznaSzerokosc == 3 && bocznaWysokosc == 3)
                    tekstury.setColor(kolorTla);
                else
                    tekstury.setColor(Color.black);
                tekstury.drawPolygon(wypelnijNaX, wypelnijNaY, 4);
            }
        }

        // GOSPODAROWANIE SCIAN POMIEDZY WARSTWAMI
        for (int i = 0; i < 6; i++) {
            int bocznaSzerokosc = klocki[i][0][1] - klocki[i][0][0];
            int bocznaWysokosc = klocki[i][1][1] - klocki[i][1][0];
            if (bocznaSzerokosc <= 0 || bocznaWysokosc <= 0) {
                for (int j = 0; j < 4; j++) {
                    int k = naroznikiScianPrzeciwnych[i][j];
                    wypelnijNaX[j] = (int) (koordynatyXowe[i][j] + (koordynatyXowe[i ^ 1][k] - koordynatyXowe[i][j]) * 2.0 / 3.0);
                    wypelnijNaY[j] = (int) (koordynatyYkowe[i][j] + (koordynatyYkowe[i ^ 1][k] - koordynatyYkowe[i][j]) * 2.0 / 3.0);

                }

                // KOLOROWANIE OBSZAROW MIEDZY SCIANAMI NA SZARO
                tekstury.setColor(Color.gray);
                tekstury.fillPolygon(wypelnijNaX, wypelnijNaY, 4);
            } else {



                // POBIERANIE DANYCH NAROZNIKOW
                for (int j = 0; j < 4; j++)
                    pobierzNarozniki(i, j, wypelnijNaX, wypelnijNaY, klocki[i][0][stopnie[j][0]], klocki[i][1][stopnie[j][1]], dokonywanieLustrzanegoOdbicia);

                // USTAWIENIE SZAREGO TLA PRZY OBROCIE
                tekstury.setColor(Color.gray);
                tekstury.fillPolygon(wypelnijNaX, wypelnijNaY, 4);
            }
        }

        /***************************************PETLA TWORZACA KLOCKI KOSTKI******************************************/
        for (int a = 0; a < 6; a++) {
            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia1, eye), 5.0), wektoryNormalneScianKostki[a]);

            // RYSOWANIE SCIAN WIDOCZNYCH
            if (tworzenieWektorow(punktWidzenia1, wektoryNormalneScianKostki[a]) > 0) {
                int bocznaSzerokosc = klocki[a][0][1] - klocki[a][0][0];
                int bocznaWysokosc = klocki[a][1][1] - klocki[a][1][0];

                if (bocznaSzerokosc > 0 && bocznaWysokosc > 0) {
                    for (int b = 0, c = klocki[a][1][0]; b < bocznaWysokosc; b++, c++) {
                        for (int d = 0, e = klocki[a][0][0]; d < bocznaSzerokosc; d++, e++) {
                            for (int f = 0; f < 4; f++)
                                pobierzNarozniki(a, f, wypelnijNaX, wypelnijNaY, e + granica[f][0], c + granica[f][1], dokonywanieLustrzanegoOdbicia);
                            tekstury.setColor(barwy[kostka[a][c * 3 + e]].darker());
                            tekstury.drawPolygon(wypelnijNaX, wypelnijNaY, 4);
                            tekstury.setColor(barwy[kostka[a][c * 3 + e]]);
                            tekstury.fillPolygon(wypelnijNaX, wypelnijNaY, 4);
                        }
                    }
                }

                // ZMIENNE ZAWIERAJACE KIERUNKI POZIOME I PIONOWE
                double kierunekPoziomyNaX = (koordynatyXowe[a][1] - koordynatyXowe[a][0] + koordynatyXowe[a][2] - koordynatyXowe[a][3]) / 6.0;
                double kierunekPoziomyNaY = (koordynatyXowe[a][3] - koordynatyXowe[a][0] + koordynatyXowe[a][2] - koordynatyXowe[a][1]) / 6.0;
                double kierunekPionowyNaX = (koordynatyYkowe[a][1] - koordynatyYkowe[a][0] + koordynatyYkowe[a][2] - koordynatyYkowe[a][3]) / 6.0;
                double kierunekPionowyNaY = (koordynatyYkowe[a][3] - koordynatyYkowe[a][0] + koordynatyYkowe[a][2] - koordynatyYkowe[a][1]) / 6.0;

                // TRYB NORMALNEGO USTAWIENIA
                if (tryb == 3) {
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 4; k++)
                            pobierzNarozniki(a, k, przeciaganieNaroznikowNaX[obszarPrzeciaganiaMysza], przeciaganieNaroznikowNaY[obszarPrzeciaganiaMysza],
                                    przeciaganieKlockow[j][k][0], przeciaganieKlockow[j][k][1], false);
                        kierunekPrzeciaganiaNaX[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaX * kierunkiPola[j][0] + kierunekPionowyNaX * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        kierunekPrzeciaganiaNaY[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaY * kierunkiPola[j][0] + kierunekPionowyNaY * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        przeciaganeWarstwy[obszarPrzeciaganiaMysza] = scianyPrzylegle[a][j % 4];
                        if (j >= 4)
                            przeciaganeWarstwy[obszarPrzeciaganiaMysza] &= ~1;
                        trybyPrzeciaganychWarstw[obszarPrzeciaganiaMysza] = j / 4;
                        obszarPrzeciaganiaMysza++;
                        if (obszarPrzeciaganiaMysza == 18)
                            break;
                    }
                }

                // TRYB KTORY POZWALA NA OBROT WARSTWA GORNA
                else if (tryb == 0) {
                    if (a != warstwaObracana && bocznaSzerokosc > 0 && bocznaWysokosc > 0) {
                        int j = bocznaSzerokosc == 3 ? (klocki[a][1][0] == 0 ? 0 : 2) : (klocki[a][0][0] == 0 ? 3 : 1);
                        for (int k = 0; k < 4; k++)
                            pobierzNarozniki(a, k, przeciaganieNaroznikowNaX[obszarPrzeciaganiaMysza], przeciaganieNaroznikowNaY[obszarPrzeciaganiaMysza],
                                    przeciaganieKlockow[j][k][0], przeciaganieKlockow[j][k][1], false);
                        kierunekPrzeciaganiaNaX[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaX * kierunkiPola[j][0] + kierunekPionowyNaX * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        kierunekPrzeciaganiaNaY[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaY * kierunkiPola[j][0] + kierunekPionowyNaY * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        przeciaganeWarstwy[obszarPrzeciaganiaMysza] = warstwaObracana;
                        trybyPrzeciaganychWarstw[obszarPrzeciaganiaMysza] = 0;
                        obszarPrzeciaganiaMysza++;
                    }
                }

                // TRYB KTORY POZWALA NA OBROT WARSTWA SRODKOWA
                else if (tryb == 1) {
                    if (a != warstwaObracana && bocznaSzerokosc > 0 && bocznaWysokosc > 0) {
                        int j = bocznaSzerokosc == 3 ? 4 : 5;
                        for (int k = 0; k < 4; k++)
                            pobierzNarozniki(a, k, przeciaganieNaroznikowNaX[obszarPrzeciaganiaMysza], przeciaganieNaroznikowNaY[obszarPrzeciaganiaMysza],
                                    przeciaganieKlockow[j][k][0], przeciaganieKlockow[j][k][1], false);
                        kierunekPrzeciaganiaNaX[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaX * kierunkiPola[j][0] + kierunekPionowyNaX * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        kierunekPrzeciaganiaNaY[obszarPrzeciaganiaMysza] = (kierunekPoziomyNaY * kierunkiPola[j][0] + kierunekPionowyNaY * kierunkiPola[j][1]) * kierunkiObrotu[a][j];
                        przeciaganeWarstwy[obszarPrzeciaganiaMysza] = warstwaObracana;
                        trybyPrzeciaganychWarstw[obszarPrzeciaganiaMysza] = 1;
                        obszarPrzeciaganiaMysza++;
                    }
                }
            }
        }
    }

    private void pobierzNarozniki(int sciana, int naroznik, int[] naroznikWspX, int[] naroznikWspY, double czynnikPierwszy, double czynnikDrugi, boolean lustro) {
        czynnikPierwszy /= 3.0;
        czynnikDrugi /= 3.0;
        double pierwszaWspNaX = koordynatyXowe[sciana][0] + (koordynatyXowe[sciana][1] - koordynatyXowe[sciana][0]) * czynnikPierwszy;
        double pierwszaWspNaY = koordynatyYkowe[sciana][0] + (koordynatyYkowe[sciana][1] - koordynatyYkowe[sciana][0]) * czynnikPierwszy;
        double drugaWspNaX = koordynatyXowe[sciana][3] + (koordynatyXowe[sciana][2] - koordynatyXowe[sciana][3]) * czynnikPierwszy;
        double drugaWspNaY = koordynatyYkowe[sciana][3] + (koordynatyYkowe[sciana][2] - koordynatyYkowe[sciana][3]) * czynnikPierwszy;
        naroznikWspX[naroznik] = (int) (0.5 + pierwszaWspNaX + (drugaWspNaX - pierwszaWspNaX) * czynnikDrugi);
        naroznikWspY[naroznik] = (int) (0.5 + pierwszaWspNaY + (drugaWspNaY - pierwszaWspNaY) * czynnikDrugi);
        if (lustro)
            naroznikWspX[naroznik] = szerokosc - naroznikWspX[naroznik];
    }

    /***********************************CO SIE DZIEJE PO NACISNIECIU I ZWOLNIENIU MYSZY********************************/
    public void mousePressed(MouseEvent e) {

        wcześniejszePolozenieMyszyNaX = wczesniejszaWspolrzednaX = e.getX();
        wcześniejszePolozenieMyszyNaY = wczesniejszaWspolrzednaY = e.getY();
        stanGdzieMoznaObracacWarstwami = false;

        if (zmianaWysokosci > 0 && wczesniejszaWspolrzednaY >= wysokosc - zmianaWysokosci && wczesniejszaWspolrzednaY < wysokosc) {
                repaint();
            }
        else {
                if (dokonywanieZmianNaKostce && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 && (e.getModifiers() & InputEvent.SHIFT_MASK) == 0)
                    stanGdzieMoznaObracacWarstwami = true;
            }

    }

    public void mouseReleased(MouseEvent e) {
        przeciaganie = false;

        // CO SIE DZIEJE POD KONIEC OBROTU WARSTWY
        if (dokonywanieObrotuWarstwy) {
            dokonywanieObrotuWarstwy = false;
            katPrzedObrotem += katPoObrocie;
            katPoObrocie = 0.0;
            double kat = katPrzedObrotem;
            while (kat < 0.0)
                kat += 32.0 * Math.PI;

            int liczba = (int) (kat * 8.0 / Math.PI) % 16;
            if (liczba % 4 == 0 || liczba % 4 == 3) {
                liczba = (liczba + 1) / 4; //
                if (kierunkiScianPrzyObracaniu[warstwaObracana] > 0)
                    liczba = (4 - liczba) % 4;
                katPrzedObrotem = 0;

                // KOSTKA JEST NIERUCHOMA
                stanObojetny = true;
                obracanieWarstw(kostka, warstwaObracana, liczba, obracanie);
            }
            repaint();
        }
    }

    /******************************************TAKIE TAM FUNKCJE******************************************************/
    private final double[] pomocniczyObserwator = new double[3];

    public void mouseMoved(MouseEvent e) {
        int a = e.getX();
        int b = e.getY();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /*********************************************PRZECIAGANIE MYSZA**************************************************/
    public void mouseDragged(MouseEvent e) {

        int polozenieX = e.getX();
        int polozenieY = e.getY();
        int wspXPrzed = polozenieX - wczesniejszaWspolrzednaX;
        int wspYPrzed = polozenieY - wczesniejszaWspolrzednaY;

        // WYBRALEM WARSTWE, NACISNALEM MYSZA ALE JESZCZE NIE OBRACAM WARSTWY
        if (dokonywanieZmianNaKostce && stanGdzieMoznaObracacWarstwami && !dokonywanieObrotuWarstwy) {
            wcześniejszePolozenieMyszyNaX = polozenieX;
            wcześniejszePolozenieMyszyNaY = polozenieY;

            // SPRAWDZAM CZY ZNAJDUJE SIE W ODPOWIEDNIM POLU PRZECIAGANIA
            for (int i = 0; i < obszarPrzeciaganiaMysza; i++) {
                double zmiennaKierunkowa1 = przeciaganieNaroznikowNaX[i][0];
                double wspXObecna = przeciaganieNaroznikowNaX[i][1] - zmiennaKierunkowa1;
                double wspYObecna = przeciaganieNaroznikowNaX[i][3] - zmiennaKierunkowa1;
                double zmiennaKierunkowa2 = przeciaganieNaroznikowNaY[i][0];
                double drugaWspX = przeciaganieNaroznikowNaY[i][1] - zmiennaKierunkowa2;
                double drugaWspY = przeciaganieNaroznikowNaY[i][3] - zmiennaKierunkowa2;
                double a = (drugaWspY * (wczesniejszaWspolrzednaX - zmiennaKierunkowa1) - wspYObecna * (wczesniejszaWspolrzednaY - zmiennaKierunkowa2)) / (wspXObecna * drugaWspY - wspYObecna * drugaWspX);
                double b = (-drugaWspX * (wczesniejszaWspolrzednaX - zmiennaKierunkowa1) + wspXObecna * (wczesniejszaWspolrzednaY - zmiennaKierunkowa2)) / (wspXObecna * drugaWspY - wspYObecna * drugaWspX);

          // POLE PRZECIAGANIA ZAWIERA KURSOR
                if (a > 0 && a < 1 && b > 0 && b < 1) {
                    przeciaganiePoX = kierunekPrzeciaganiaNaX[i];
                    przeciaganiePoY = kierunekPrzeciaganiaNaY[i];
                    double d = Math.abs(przeciaganiePoX * wspXPrzed + przeciaganiePoY * wspYPrzed) / Math.sqrt((przeciaganiePoX * przeciaganiePoX + przeciaganiePoY * przeciaganiePoY) * (wspXPrzed * wspXPrzed + wspYPrzed * wspYPrzed));
                    if (d > 0.75) {
                        dokonywanieObrotuWarstwy = true;
                        warstwaObracana = przeciaganeWarstwy[i];
                        obracanie = trybyPrzeciaganychWarstw[i];
                        break;
                    }
                }
            }

            stanGdzieMoznaObracacWarstwami = false;
            wczesniejszaWspolrzednaX = wcześniejszePolozenieMyszyNaX;
            wczesniejszaWspolrzednaY = wcześniejszePolozenieMyszyNaY;
        }

        wspXPrzed = polozenieX - wczesniejszaWspolrzednaX;
        wspYPrzed = polozenieY - wczesniejszaWspolrzednaY;

        // DOKONUJE OBROTU KOSTKA, NIE OBROTU WARSTWA
        if (!dokonywanieObrotuWarstwy) {
            wektorNormalny(dodawanieWektorow(obserwator, skalowanieWektorow(kopiowanieWektorow(pomocniczyObserwator, widokBokiem), wspXPrzed * -0.0016)));
            wektorNormalny(mnozenieWektorow(widokBokiem, widokPionowy, obserwator));
            wektorNormalny(dodawanieWektorow(obserwator, skalowanieWektorow(kopiowanieWektorow(pomocniczyObserwator, widokPionowy), wspYPrzed * 0.0016)));
            wektorNormalny(mnozenieWektorow(widokPionowy, obserwator, widokBokiem));
            wczesniejszaWspolrzednaX = polozenieX;
            wczesniejszaWspolrzednaY = polozenieY;
        }
        else {
            if (stanObojetny)
                warstwyKostki(warstwaObracana);
            katPoObrocie = 0.005 * (przeciaganiePoX * wspXPrzed + przeciaganiePoY * wspYPrzed) / Math.sqrt(przeciaganiePoX * przeciaganiePoX + przeciaganiePoY * przeciaganiePoY);
        }


        repaint();
    }



    /*************************FUNKCJE WYKONYWANE NAD WEKTORAMI********************************************************/
    private static double[] kopiowanieWektorow(double[] vector, double[] srcVec) {
        vector[0] = srcVec[0];
        vector[1] = srcVec[1];
        vector[2] = srcVec[2];
        return vector;
    }

    private static double[] wektorNormalny(double[] wektor) {
        double dlugoscWektora = Math.sqrt(tworzenieWektorow(wektor, wektor));
        wektor[0] /= dlugoscWektora;
        wektor[1] /= dlugoscWektora;
        wektor[2] /= dlugoscWektora;
        return wektor;
    }

    private static double[] skalowanieWektorow(double[] wektor, double liczba) {
        wektor[0] *= liczba;
        wektor[1] *= liczba;
        wektor[2] *= liczba;
        return wektor;
    }

    private static double tworzenieWektorow(double[] wektor1, double[] wektor2) {
        return wektor1[0] * wektor2[0] + wektor1[1] * wektor2[1] + wektor1[2] * wektor2[2];
    }

    private static double[] dodawanieWektorow(double[] wektor, double[] wektorOryginalny) {
        wektor[0] += wektorOryginalny[0];
        wektor[1] += wektorOryginalny[1];
        wektor[2] += wektorOryginalny[2];
        return wektor;
    }

    private static double[] odejmowanieWektorow(double[] wektor, double[] wektorOryginalny) {
        wektor[0] -= wektorOryginalny[0];
        wektor[1] -= wektorOryginalny[1];
        wektor[2] -= wektorOryginalny[2];
        return wektor;
    }

    private static double[] mnozenieWektorow(double[] wektor, double[] wektor1, double[] wektor2) {
        wektor[0] = wektor1[1] * wektor2[2] - wektor1[2] * wektor2[1];
        wektor[1] = wektor1[2] * wektor2[0] - wektor1[0] * wektor2[2];
        wektor[2] = wektor1[0] * wektor2[1] - wektor1[1] * wektor2[0];
        return wektor;
    }

    @Override
    public void run() {

    }
}
