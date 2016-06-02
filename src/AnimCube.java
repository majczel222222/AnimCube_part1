import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.Hashtable;


/*  natural             = stanObojetny
    bgColor             = kolorTla
    buttonBgColor       = przyciskKoloruTla
    colors              = barwy
    cube                = kostka
    initialCube         = stanPoczatkowyKostki
    faceNormals         = wektoryNormalneScianKostki
    cornerCoords        = wspolrzedneNaroznikowKostki
    faceCorners         = naroznikiScianKostki
    oppositeCorners     = naroznikiScianPrzeciwnych
    adjacentFaces       = scianyPrzylegle
    twistedLayer        = warstwaObracana
    twistedMode         = obracanie
    faceTwistDirs       = kierunkiScianPrzyObracaniu


*/


public final class AnimCube extends Applet implements Runnable, MouseListener, MouseMotionListener {


    // zewnetrzna konfiguracja
    private final Hashtable konfiguracja = new Hashtable();


    // kolory tla
    private Color kolorTla;
    private Color przyciskKoloruTla;


    // kolory kostki
    private final Color[] barwy = new Color[24];


    // kostka facelets

    private final int[][] kostka = new int[6][9];
    private final int[][] stanPoczatkowyKostki = new int[6][9];


    // wektory normalne
    private static final double[][] wektoryNormalneScianKostki = {
            {0.0, -1.0, 0.0}, // Up
            {0.0, 1.0, 0.0}, // Down
            {0.0, 0.0, -1.0}, // Front
            {0.0, 0.0, 1.0}, // Back
            {-1.0, 0.0, 0.0}, // Left
            {1.0, 0.0, 0.0}  // Right
    };


    // wierzcholkow wspolrzedne
    private static final double[][] wspolrzedneNaroznikowKostki = {
            {-1.0, -1.0, -1.0}, // UFL
            {1.0, -1.0, -1.0}, // UFR
            {1.0, -1.0, 1.0}, // UBR
            {-1.0, -1.0, 1.0}, // UBL
            {-1.0, 1.0, -1.0}, // DFL
            {1.0, 1.0, -1.0}, // DFR
            {1.0, 1.0, 1.0}, // DBR
            {-1.0, 1.0, 1.0}  // DBL
    };


    // wierzcholki kazdej sciany
    private static final int[][] naroznikiScianKostki = {
            {0, 1, 2, 3}, // U: UFL UFR UBR UBL
            {4, 7, 6, 5}, // D: DFL DBL DBR DFR
            {0, 4, 5, 1}, // F: UFL DFL DFR UFR
            {2, 6, 7, 3}, // B: UBR DBR DBL UBL
            {0, 3, 7, 4}, // L: UFL UBL DBL DFL
            {1, 5, 6, 2}  // R: UFR DFR DBR UBR
    };


    // odpowiadajace ze soba rogi na przeciwnych scianach
    private static final int[][] naroznikiScianPrzeciwnych = {
            {0, 3, 2, 1}, // U->D  GORA->DOL
            {0, 3, 2, 1}, // D->U  DOL->GORA
            {3, 2, 1, 0}, // F->B  PRZOD->TYL
            {3, 2, 1, 0}, // B->F  TYL->PRZOD
            {0, 3, 2, 1}, // L->R  LEWO->PRAWO
            {0, 3, 2, 1}, // R->L  PRAWO->LEWO
    };


    // sciany przylegajace do kazdej sciany
    private static final int[][] scianyPrzylegle = {
            {2, 5, 3, 4}, // U: F R B L  GORA:  PRZOD PRAWO TYL LEWO
            {4, 3, 5, 2}, // D: L B R F  DOL:   LEWO TYL PRAWO PRZOD
            {4, 1, 5, 0}, // F: L D R U  PRZOD: LEWO DOL PRAWO GORA
            {5, 1, 4, 0}, // B: R D L U  TYL:   PRAWO DOL LEWO GORA
            {0, 3, 1, 2}, // L: U B D F  LEWO: GORA TYL DOL PRZOD
            {2, 1, 3, 0}  // R: F D B U  PRAWO: PRZOD DOL TYL GORA
    };


    // obecna przekrecana warstwa
    private int warstwaObracana;
    private int obracanie;


    // directions of facelet cycling for all faces
    private static final int[] kierunkiScianPrzyObracaniu = {1, 1, -1, -1, -1, -1};


    // wstepne osie wspolrzednych obserwatora (widok)
    // eye = obserwator
    // eyeX = widokBokiem
    // eyeY = widokPionowy
    // initialEye = wstepnyObserwator
    // initialEyeX = wstepnyWidokBokiem
    // initialEyeY = wstepnyWidokPionowy
    private final double[] obserwator = {0.0, 0.0, -1.0};
    private final double[] widokBokiem = {1.0, 0.0, 0.0}; // (bokiem)
    private final double[] widokPionowy = new double[3]; // (pionowo)
    private final double[] wstepnyObserwator = new double[3];
    private final double[] wstepnyWidokBokiem = new double[3];
    private final double[] wstepnyWidokPionowy = new double[3];


    // kat obrotu obracanej warstwy
    // currentAngle = katPoObrocie
    // originalAngle = katPrzedObrotem
    private double katPoObrocie; // edited angle of twisted layer
    private double katPrzedObrotem; // angle of twisted layer


    // animation speed
    // toTwist1 = stanGdzieMoznaObracacWarstwami
    // mirrored1 = dokonywanieLustrzanegoOdbicia
    // editable1 = dokonywanieZmianNaKostce
    // twisting1 = dokonywanieObrotuWarstwy
    // dragging1 = przeciaganie
    private boolean stanObojetny = true; // kostka jest w bezruchu, żadna warstwa niej jest obracana
    private boolean stanGdzieMoznaObracacWarstwami; // layer can be twisted
    private boolean dokonywanieLustrzanegoOdbicia; // mirroring of the kostka view
    private boolean dokonywanieZmianNaKostce; // editation of the kostka with a mouse
    private boolean dokonywanieObrotuWarstwy; // a user twists a kostka layer
    private boolean przeciaganie; // progress bar is controlled


    //private double scale; // kostka scale
    // align = ustawienieKostki
    private int ustawienieKostki; // kostka alignment (top, center, bottom)


    // ruch sequence data
    // move1 = ruch
    // curMove1 = poruszanie
    // movePos1 = torRuchu
    // moveDir1 = kierunekRuchu


    private int torRuchu;



    // state of buttons
    // buttonBar = PasekPrzyciskow
    // buttonHeight = wysokoscPrzyciskow
    // drawButtons1 = tworzeniePrzyciskow
    // pushed1 = przycisniecie
    // buttonPressed1 = wcisnietyPrzycisk
    // progressHeight1 = zmianaWysokosci
    private int PasekPrzyciskow; // button bar mode
    private int wysokoscPrzyciskow;
    private boolean tworzeniePrzyciskow = true;
    private boolean przycisniecie;
    private int wcisnietyPrzycisk = -1;
    private int zmianaWysokosci = 6;


    // private int textHeight;
    //Random generator = new Random();
    //private boolean outlined = true;

    // buffer to store hexa-digits
    //private final int[] hex = new int[6];


    public void init() {


        // register to receive all mouse events
        addMouseListener(this);
        addMouseMotionListener(this);


        // setup barwy
        barwy[0] = new Color(255, 84, 0);   // 0 -   orange
        barwy[1] = new Color(255, 0, 0);      // 1 - red
        barwy[2] = new Color(0, 255, 0);      // 2 - green
        barwy[3] = new Color(0, 0, 255);      // 3 - blue
        barwy[4] = new Color(255, 255, 255);  // 4 - white
        barwy[5] = new Color(255, 255, 0);   // 5 - yellow


        // setup default configuration
        // param = wspolczynnikKonfiguracji

        String wspolczynnikKonfiguracji = getParameter("konfiguracja");
        kolorTla = Color.DARK_GRAY;
        przyciskKoloruTla = kolorTla;


        // clean the kostka
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 9; j++) {
                //int k = generator.nextInt(9);
                kostka[i][j] = i;
            }


        // setup ruch sequence (and info texts)


        wspolczynnikKonfiguracji = getParameter("ruch");

        torRuchu = 0;


        // appearance and configuration of the button bar
        PasekPrzyciskow = 1;
        wysokoscPrzyciskow = 13;
        wektorNormalny(mnozenieWektorow(widokPionowy, obserwator, widokBokiem)); // fix widokPionowy


        //zmianaWysokosci = ruch.length == 0 ? 0 : 6;



        /* COS SIE ODJANIEPAWLILO */
        //wspolczynnikKonfiguracji = getParameter("buttonbar");
       /* if ("0".equals(wspolczynnikKonfiguracji)) {
            PasekPrzyciskow = 0;
            wysokoscPrzyciskow = 0;
        }*/


        // whether the kostka can be edited with mouse
        //wspolczynnikKonfiguracji = getParameter("edit");

        if ("0".equals(wspolczynnikKonfiguracji))
            dokonywanieZmianNaKostce = false;
        else
            dokonywanieZmianNaKostce = true;


        // metric
        ustawienieKostki = 1;

        wspolczynnikKonfiguracji = getParameter("ustawienieKostki");
        if (wspolczynnikKonfiguracji != null) {
            if ("0".equals(wspolczynnikKonfiguracji)) // top
                ustawienieKostki = 0;
            else if ("1".equals(wspolczynnikKonfiguracji)) // center
                ustawienieKostki = 1;
            else if ("2".equals(wspolczynnikKonfiguracji)) // bottom
                ustawienieKostki = 2;
        }


        // setup initial values
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 9; j++)
                stanPoczatkowyKostki[i][j] = kostka[i][j];

        for (int i = 0; i < 3; i++) {
            wstepnyObserwator[i] = obserwator[i];
            wstepnyWidokBokiem[i] = widokBokiem[i];
            wstepnyWidokPionowy[i] = widokPionowy[i];
        }
    }// init()

    // parameter = stala
    public String getParameter(String name) {
        String stala = super.getParameter(name);
        if (stala == null)
            return (String) konfiguracja.get(name);

        return stala;
    }




    // realMoveLength1 = dlugoscRuchuTeraz
    // move = przesuwanie
    private static int dlugoscRuchuTeraz(int[] przesuwanie) {
        int length = 0;
        for (int i = 0; i < przesuwanie.length; i++)
            if (przesuwanie[i] < 1000)
                length++;
        return length;
    }


    // realMovePos = torRuchuTeraz
    // move = przesuwanie
    // pos = stan
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



    // kostka dimensions in number of facelets (mincol, maxcol, minrow, maxrow) for compact kostka
    // klockiKostkiNieruchomej = klockiKostkiNieruchomej
    private static final int[][][] klockiKostkiNieruchomej = {
            {{0, 3}, {0, 3}}, // U
            {{0, 3}, {0, 3}}, // D
            {{0, 3}, {0, 3}}, // F
            {{0, 3}, {0, 3}}, // B
            {{0, 3}, {0, 3}}, // L
            {{0, 3}, {0, 3}}  // R
    };



    // subcube dimensions
    // topBlocks = klockiGorne
    // midBlocks = klockiSrodkowe
    // botBlocks1 = klockiDolne
    private final int[][][] klockiGorne = new int[6][][];
    private final int[][][] klockiSrodkowe = new int[6][][];
    private final int[][][] klockiDolne = new int[6][][];



    // all possible subcube dimensions for top and bottom layers
    // topBlockTable1 = daneKlockowGornych
    // midBlockTable1 = daneKlockowSrodkowych
    // topBlockFaceDim1 = wymiaryKlockowGornych
    // midBlockFaceDim1 = wymiaryKlockowSrodkowych
    // botBlockFaceDim1 = wymiaryKlockowDolnych
    private static final int[][][] daneKlockowGornych = {
            {{0, 0}, {0, 0}},
            {{0, 3}, {0, 3}},
            {{0, 3}, {0, 1}},
            {{0, 1}, {0, 3}},
            {{0, 3}, {2, 3}},
            {{2, 3}, {0, 3}}
    };
    // subcube dimmensions for middle layers
    private static final int[][][] daneKlockowSrodkowych = {
            {{0, 0}, {0, 0}},
            {{0, 3}, {1, 2}},
            {{1, 2}, {0, 3}}
    };
    // indices to daneKlockowGornych[] and botBlockTable[] for each warstwaObracana value
    private static final int[][] wymiaryKlockowGornych = {
            // U  D  F  B  L  R
            {1, 0, 3, 3, 2, 3}, // U
            {0, 1, 5, 5, 4, 5}, // D
            {2, 3, 1, 0, 3, 2}, // F
            {4, 5, 0, 1, 5, 4}, // B
            {3, 2, 2, 4, 1, 0}, // L
            {5, 4, 4, 2, 0, 1}  // R
    };
    private static final int[][] wymiaryKlockowSrodkowych = {
            // U  D  F  B  L  R
            {0, 0, 2, 2, 1, 2}, // U
            {0, 0, 2, 2, 1, 2}, // D
            {1, 2, 0, 0, 2, 1}, // F
            {1, 2, 0, 0, 2, 1}, // B
            {2, 1, 1, 1, 0, 0}, // L
            {2, 1, 1, 1, 0, 0}  // R
    };
    private static final int[][] wymiaryKlockowDolnych = {
            // U  D  F  B  L  R
            {0, 1, 5, 5, 4, 5}, // U
            {1, 0, 3, 3, 2, 3}, // D
            {4, 5, 0, 1, 5, 4}, // F
            {2, 3, 1, 0, 3, 2}, // B
            {5, 4, 4, 2, 0, 1}, // L
            {3, 2, 2, 4, 1, 0}  // R
    };






    // splitCube = warstwyKostki
    // layer1 = warstwa
    private void warstwyKostki(int warstwa) {
        for (int i = 0; i < 6; i++) { // dla wszystkich scian
            klockiGorne[i] = daneKlockowGornych[wymiaryKlockowGornych[warstwa][i]];
            klockiDolne[i] = daneKlockowGornych[wymiaryKlockowDolnych[warstwa][i]];
            klockiSrodkowe[i] = daneKlockowSrodkowych[wymiaryKlockowSrodkowych[warstwa][i]];
        }
        stanObojetny = false;
    }




    // twistLayers = obracanieWarstw
    // cube1 = kostka
    // layer1 = warstwa
    // num1 = liczbaPorzadkowa
    // mode1 = stan
    // twistLayer = obracanieWarstwy
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



    // top facelet cycle
    // cycleOrder = kolejnoscOkresu
    // cycleFactors = stopnieOkresu
    // cycleOffsets1 = rozstawienieOkresu
    // cycleLayerSides1 = warstwyBoczneOkresu
    // cycleCenters1 = srodekOkresu
    private static final int[] kolejnoscOkresu = {0, 1, 2, 5, 8, 7, 6, 3};
    // side facelet cycle offsets
    private static final int[] stopnieOkresu = {1, 3, -1, -3, 1, 3, -1, -3};
    private static final int[] rozstawienieOkresu = {0, 2, 8, 6, 3, 1, 5, 7};
    // indices for faces of layers
    private static final int[][] warstwyBoczneOkresu = {
            {3, 3, 3, 0}, // U: F=6-3k R=6-3k B=6-3k L=k
            {2, 1, 1, 1}, // D: L=8-k  B=2+3k R=2+3k F=2+3k
            {3, 3, 0, 0}, // F: L=6-3k D=6-3k R=k    U=k
            {2, 1, 1, 2}, // B: R=8-k  D=2+3k L=2+3k U=8-k
            {3, 2, 0, 0}, // L: U=6-3k B=8-k  D=k    F=k
            {2, 2, 0, 1}  // R: F=8-k  D=8-k  B=k    U=2+3k
    };
    // indices for sides of center layers
    private static final int[][] srodekOkresu = {
            {7, 7, 7, 4}, // E'(U): F=7-3k R=7-3k B=7-3k L=3+k
            {6, 5, 5, 5}, // E (D): L=5-k  B=1+3k R=1+3k F=1+3k
            {7, 7, 4, 4}, // S (F): L=7-3k D=7-3k R=3+k  U=3+k
            {6, 5, 5, 6}, // S'(B): R=5-k  D=1+3k L=1+3k U=5-k
            {7, 6, 4, 4}, // M (L): U=7-3k B=8-k  D=3+k  F=3+k
            {6, 6, 4, 5}  // M'(R): F=5-k  D=5-k  B=3+k  U=1+3k
    };





    // twistBuffer1 = zmianaBuforu
    private final int[] zmianaBuforu = new int[12];



    // cube1 = kostka
    // layer1 = warstwa
    // num1 = liczbaPorzadkowa
    // middle = srodek
    private void obracanieWarstwy(int[][] kostka, int warstwa, int liczbaPorzadkowa, boolean srodek) {
        if (!srodek) {
            // rotate top facelets
            for (int i = 0; i < 8; i++) // to buffer
                zmianaBuforu[(i + liczbaPorzadkowa * 2) % 8] = kostka[warstwa][kolejnoscOkresu[i]];
            for (int i = 0; i < 8; i++) // to kostka
                kostka[warstwa][kolejnoscOkresu[i]] = zmianaBuforu[i];
        }
        // rotate side facelets
        int z = liczbaPorzadkowa * 3;
        for (int i = 0; i < 4; i++) { // to buffer
            int x = scianyPrzylegle[warstwa][i];
            int y = srodek ? srodekOkresu[warstwa][i] : warstwyBoczneOkresu[warstwa][i];
            int stopien = stopnieOkresu[y];
            int rozstaw = rozstawienieOkresu[y];
            for (int j = 0; j < 3; j++) {
                zmianaBuforu[z % 12] = kostka[x][j * stopien + rozstaw];
                z++;
            }
        }
        z = 0; // MS VM JIT bug if placed into the loop init
        for (int i = 0; i < 4; i++) { // to kostka
            int x = scianyPrzylegle[warstwa][i];
            int y = srodek ? srodekOkresu[warstwa][i] : warstwyBoczneOkresu[warstwa][i];
            int stopien = stopnieOkresu[y];
            int rozstaw = rozstawienieOkresu[y];
            //int j = 0; // MS VM JIT bug if for is used
            for (int j = 0; j < 3; j++) {
                kostka[x][j * stopien + rozstaw] = zmianaBuforu[z];
                z++;
            }
        }
    }






    // double buffered animation
    // graphics = tekstury
    // image1 = obraz
    private Graphics tekstury = null;
    private Image obraz = null;


    // kostka window size (applet window is resizable)
    // width = szerokosc
    // height1 = wysokosc
    private int szerokosc;
    private int wysokosc;


    // last position of mouse (for przeciaganie the kostka)
    // lastX = wczesniejszaWspolrzednaX
    // lastY1 = wczesniejszaWspolrzednaY
    private int wczesniejszaWspolrzednaX;
    private int wczesniejszaWspolrzednaY;


    // last position of mouse (when waiting for clear decission)
    // lastDragX = wcześniejszePolozenieMyszyNaX
    // lastDragY1 = wcześniejszePolozenieMyszyNaY
    private int wcześniejszePolozenieMyszyNaX;
    private int wcześniejszePolozenieMyszyNaY;


    // drag areas
    // dragAreas1 = obszarPrzeciaganiaMysza
    // dragCornersX1 = przeciaganieNaroznikowNaX
    // dragCornersY1 = przeciaganieNaroznikowNaY
    // dragDirsX1 = kierunekPrzeciaganiaNaX
    // dragDirsY1 = kierunekPrzeciaganiaNaY
    // dragBlocks1 = przeciaganieKlockow
    private int obszarPrzeciaganiaMysza;
    private final int[][] przeciaganieNaroznikowNaX = new int[18][4];
    private final int[][] przeciaganieNaroznikowNaY = new int[18][4];
    private final double[] kierunekPrzeciaganiaNaX = new double[18];
    private final double[] kierunekPrzeciaganiaNaY = new double[18];
    private static final int[][][] przeciaganieKlockow = {
            {{0, 0}, {3, 0}, {3, 1}, {0, 1}},
            {{3, 0}, {3, 3}, {2, 3}, {2, 0}},
            {{3, 3}, {0, 3}, {0, 2}, {3, 2}},
            {{0, 3}, {0, 0}, {1, 0}, {1, 3}},
            // center slices
            {{0, 1}, {3, 1}, {3, 2}, {0, 2}},
            {{2, 0}, {2, 3}, {1, 3}, {1, 0}}
    };


    // kierunkiPola =
    // kierunkiObrotu =
    private static final int[][] kierunkiPola = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 0}, {0, 1}};
    private static final int[][] kierunkiObrotu = {
            {1, 1, 1, 1, 1, -1}, // U
            {1, 1, 1, 1, 1, -1}, // D
            {1, -1, 1, -1, 1, 1}, // F
            {1, -1, 1, -1, -1, 1}, // B
            {-1, 1, -1, 1, -1, -1}, // L
            {1, -1, 1, -1, 1, 1}  // R
    };


    // dragLayers1 = przeciaganeWarstwy
    // dragModes1 = trybyPrzeciaganychWarstw
    private int[] przeciaganeWarstwy = new int[18]; // which layers belongs to dragCorners
    private int[] trybyPrzeciaganychWarstw = new int[18]; // which layer modes dragCorners


    // current drag directions
    // dragX1 = przeciaganiePoX
    // dragY1 = przeciaganiePoY
    private double przeciaganiePoX;
    private double przeciaganiePoY;


    // various sign tables for computation of directions of rotations
    // rotCos1 = odwracanieCosinusa
    // rotSin1 = odwracanieSinusa
    // rotVec1 = odwracanieWektora
    // rotSign1 = odwracanieZnaku
    private static final int[][][] odwracanieCosinusa = {
            {{1, 0, 0}, {0, 0, 0}, {0, 0, 1}}, // U-D
            {{1, 0, 0}, {0, 1, 0}, {0, 0, 0}}, // F-B
            {{0, 0, 0}, {0, 1, 0}, {0, 0, 1}}  // L-R
    };
    private static final int[][][] odwracanieSinusa = {
            {{0, 0, 1}, {0, 0, 0}, {-1, 0, 0}}, // U-D
            {{0, 1, 0}, {-1, 0, 0}, {0, 0, 0}}, // F-B
            {{0, 0, 0}, {0, 0, 1}, {0, -1, 0}}  // L-R
    };
    private static final int[][][] odwracanieWektora = {
            {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}}, // U-D
            {{0, 0, 0}, {0, 0, 0}, {0, 0, 1}}, // F-B
            {{1, 0, 0}, {0, 0, 0}, {0, 0, 0}}  // L-R
    };


    private static final int[] odwracanieZnaku = {1, -1, 1, -1, 1, -1}; // U, D, F, B, L, R


    // temporary obserwator vectors for twisted sub-kostka rotation

    private double[] tymczasowyObserwator = new double[3];
    private double[] tymczasowyWidokNaX = new double[3];
    private double[] tymczasowyWidokNaY = new double[3];


    // temporary obserwator vectors for second twisted sub-kostka rotation (antislice)

    private double[] tymczasowyDrugiObserwator = new double[3];
    private double[] tymczasowyDrugiWidokNaX = new double[3];
    private final double[] tymczasowyDrugiWidokNaY = new double[3];


    // temporary vectors to compute visibility in perspective projection

    private double[] punktWidzenia1 = new double[3];
    private double[] punktWidzenia2 = new double[3];
    private double[] widokNormalny = new double[3];


    // obserwator arrays to store various eyes for various modes
    // eyeOrder1 = kolejnoscObserwatorow
    // blockMode1 = trybKlockow
    // drawOrder1 = tworzenieUporzadkowania
    private double[][] ustawienieObserwatora = new double[3][];
    private double[][] ustawienieWidokuNaX = new double[3][];
    private double[][] ustawienieWidokuNaY = new double[3][];
    private int[][] kolejnoscObserwatorow = {{1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 2}};
    private int[][][][] ustawienieKlockow = new int[3][][][];
    private int[][] trybKlockow = {{0, 2, 2}, {2, 1, 2}, {2, 2, 2}, {2, 2, 2}, {2, 2, 2}, {2, 2, 2}};
    private int[][] tworzenieUporzadkowania = {{0, 1, 2}, {2, 1, 0}, {0, 2, 1}};






    // size1 = rozmiar
    public void paint(Graphics g) {

        Dimension rozmiar = getSize(); // inefficient - Java 1.1


        // create offscreen buffer for double buffering
        if (obraz == null || rozmiar.width != szerokosc || rozmiar.height - wysokoscPrzyciskow != wysokosc) {
            szerokosc = 900;
            wysokosc = 1000;
            obraz = createImage(szerokosc, wysokosc);
            tekstury = obraz.getGraphics();
           // textHeight = tekstury.getFontMetrics().getHeight() - tekstury.getFontMetrics().getLeading();
            if (PasekPrzyciskow == 1)
                wysokosc -= wysokoscPrzyciskow;
            tworzeniePrzyciskow = true;
        }


        tekstury.setColor(kolorTla);
        tekstury.setClip(0, 0, szerokosc, wysokosc);
        tekstury.fillRect(0, 0, szerokosc, wysokosc);

        obszarPrzeciaganiaMysza = 0;


        // fixblock1 = ustalanieKlocka
        if (stanObojetny) // compact kostka
            ustalanieKlocka(obserwator, widokBokiem, widokPionowy, klockiKostkiNieruchomej, 3); // draw kostka and fill drag areas


        // tempEye = tymczasowyObserwator
        // tempEyeX = tymczasowyWidokNaX
        // tempEye21 = tymczasowyDrugiObserwator
        // tempEyeX21 = tymczasowyDrugiWidokNaX

        else {

            // in twisted state
            // compute top observer
            double cosA = Math.cos(katPrzedObrotem + katPoObrocie);
            double sinA = Math.sin(katPrzedObrotem + katPoObrocie) * odwracanieZnaku[warstwaObracana];
            int i = 0;
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


            // vMul1 = mnozenieWektorow
            mnozenieWektorow(tymczasowyWidokNaY, tymczasowyObserwator, tymczasowyWidokNaX);
            // compute bottom anti-observer
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


            // eyeArray1 = ustawienieObserwatora
            // eyeArrayX1 = ustawienieWidokuNaX
            // eyeArrayY1 = ustawienieWidokuNaY
            // blockArray1 = ustawienieKlockow
            // tempEyeY1 = tymczasowyWidokNaY
            // tempEyeY21 = tymczasowyDrugiWidokNaY
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


            // perspective corrections
            // perspEye1 = punktWidzenia1
            // perspNormal1 = widokNormalny
            // perspEyeI1 = punktWidzenia2
            // vSub = odejmowanieWektorow
            // vScale1 = skalowanieWektorow
            // vCopy1 = kopiowanieWektorow
            // vProd1 = tworzenieWektorow
            // topProd1 = tworzenieGornych
            // botProd1 = tworzenieDolnych
            // orderMode1 = trybPorzadkowania
            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia1, obserwator), 5.0), skalowanieWektorow(kopiowanieWektorow(widokNormalny, wektoryNormalneScianKostki[warstwaObracana]), 1.0 / 3.0));
            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia2, obserwator), 5.0), skalowanieWektorow(kopiowanieWektorow(widokNormalny, wektoryNormalneScianKostki[warstwaObracana ^ 1]), 1.0 / 3.0));
            double tworzenieGornych = tworzenieWektorow(punktWidzenia1, wektoryNormalneScianKostki[warstwaObracana]);
            double tworzenieDolnych = tworzenieWektorow(punktWidzenia2, wektoryNormalneScianKostki[warstwaObracana ^ 1]);
            int trybPorzadkowania;
            if (tworzenieGornych < 0 && tworzenieDolnych > 0) // top facing away
                trybPorzadkowania = 0;
            else if (tworzenieGornych > 0 && tworzenieDolnych < 0) // bottom facing away: draw it first
                trybPorzadkowania = 1;
            else // both top and bottom layer facing away: draw them first
                trybPorzadkowania = 2;
            ustalanieKlocka(ustawienieObserwatora[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][0]]],
                    ustawienieWidokuNaX[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][0]]],
                    ustawienieWidokuNaY[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][0]]],
                    ustawienieKlockow[tworzenieUporzadkowania[trybPorzadkowania][0]],
                    trybKlockow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][0]]);
            ustalanieKlocka(ustawienieObserwatora[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][1]]],
                    ustawienieWidokuNaX[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][1]]],
                    ustawienieWidokuNaY[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][1]]],
                    ustawienieKlockow[tworzenieUporzadkowania[trybPorzadkowania][1]],
                    trybKlockow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][1]]);
            ustalanieKlocka(ustawienieObserwatora[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][2]]],
                    ustawienieWidokuNaX[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][2]]],
                    ustawienieWidokuNaY[kolejnoscObserwatorow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][2]]],
                    ustawienieKlockow[tworzenieUporzadkowania[trybPorzadkowania][2]],
                    trybKlockow[obracanie][tworzenieUporzadkowania[trybPorzadkowania][2]]);
        }




        if (!przycisniecie) // no button should be deceased
            wcisnietyPrzycisk = -1;

        if (tworzeniePrzyciskow && PasekPrzyciskow != 0) // omit unneccessary redrawing
            tworzeniePrzyciskow(tekstury);
        /*****************************************************************************************
         * na połówkowy do wyjebania
         if (tworzeniePrzyciskow && PasekPrzyciskow != 0) // omit unneccessary redrawing
         tworzeniePrzyciskow(tekstury);
         ***********************************************************************************/

        g.drawImage(obraz, 0, 0, this);
    } // paint()

    public void update(Graphics g) {
        paint(g);
    }


    // polygon co-ordinates to fill (kostka faces or facelets)
    // fillX = wypelnijNaX
    // fillY1 = wypelnijNaY
    // coordsX1 = wspolrzedneX
    // coordsY1 = wspolrzedneY
    // cooX1 = koordynatyXowe
    // cooY1 = koordynatyYkowe
    // border1 = granica
    // factors1 = stopnie
    private final int[] wypelnijNaX = new int[4];
    private final int[] wypelnijNaY = new int[4];


    // projected vertex co-ordinates (to screen)
    private final double[] wspolrzedneX = new double[8];
    private final double[] wspolrzedneY = new double[8];
    private final double[][] koordynatyXowe = new double[6][4];
    private final double[][] koordynatyYkowe = new double[6][4];
    private static final double[][] granica = {{0.02, 0.02}, {0.98, 0.02}, {0.98, 0.98}, {0.02, 0.98}};
    private static final int[][] stopnie = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};


    private void ustalanieKlocka(double[] eye, double[] eyeX, double[] eyeY, int[][][] klocki, int tryb) {



        // project 3D co-ordinates into 2D screen ones
        // min1 = wartoscMinimalna
        // x1 = wspX
        // y1 = wspY
        // z1 = wspZ
        for (int i = 0; i < 8; i++) {
            double wartoscMinimalna = szerokosc < wysokosc ? szerokosc : wysokosc - zmianaWysokosci;
            double wspX = wartoscMinimalna / 3.7 * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eyeX);
            double wspY = wartoscMinimalna / 3.7 * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eyeY);
            double wspZ = wartoscMinimalna / (5.0) * tworzenieWektorow(wspolrzedneNaroznikowKostki[i], eye);
            wspX = wspX / (1 - wspZ / wartoscMinimalna); // perspective transformation
            wspY = wspY / (1 - wspZ / wartoscMinimalna); // perspective transformation
            wspolrzedneX[i] = szerokosc / 2.0 + wspX;
            if (ustawienieKostki == 0)
                wspolrzedneY[i] = (wysokosc - zmianaWysokosci) / 2.0 * -wspY;
            else if (ustawienieKostki == 2)
                wspolrzedneY[i] = wysokosc - zmianaWysokosci - (wysokosc - zmianaWysokosci) / 2.0 * -wspY;
            else
                wspolrzedneY[i] = (wysokosc - zmianaWysokosci) / 2.0 - wspY;
        }
        // setup corner co-ordinates for all faces
        for (int i = 0; i < 6; i++) { // all faces
            for (int j = 0; j < 4; j++) { // all face corners
                koordynatyXowe[i][j] = wspolrzedneX[naroznikiScianKostki[i][j]];
                koordynatyYkowe[i][j] = wspolrzedneY[naroznikiScianKostki[i][j]];
            }
        }

        // draw black antialias
        // sideW = bocznaSzerokosc
        // sideH = bocznaWysokosc
        // klocki = klocki
        // getCorners = pobierzNarozniki
        for (int i = 0; i < 6; i++) { // all faces
            int bocznaSzerokosc = klocki[i][0][1] - klocki[i][0][0];
            int bocznaWysokosc = klocki[i][1][1] - klocki[i][1][0];
            if (bocznaSzerokosc > 0 && bocznaWysokosc > 0) {
                for (int j = 0; j < 4; j++) // corner co-ordinates
                    pobierzNarozniki(i, j, wypelnijNaX, wypelnijNaY, klocki[i][0][stopnie[j][0]], klocki[i][1][stopnie[j][1]], dokonywanieLustrzanegoOdbicia);
                if (bocznaSzerokosc == 3 && bocznaWysokosc == 3)
                    tekstury.setColor(kolorTla);
                else
                    tekstury.setColor(Color.black);
                tekstury.drawPolygon(wypelnijNaX, wypelnijNaY, 4);
            }
        }


        // find and draw black inner faces
        // sideW1 = bocznaSzerokosc
        // sideH1 = bocznaWysokosc
        for (int i = 0; i < 6; i++) { // all faces
            int bocznaSzerokosc = klocki[i][0][1] - klocki[i][0][0];
            int bocznaWysokosc = klocki[i][1][1] - klocki[i][1][0];
            if (bocznaSzerokosc <= 0 || bocznaWysokosc <= 0) { // this face is inner and only black
                for (int j = 0; j < 4; j++) { // for all corners
                    int k = naroznikiScianPrzeciwnych[i][j];
                    wypelnijNaX[j] = (int) (koordynatyXowe[i][j] + (koordynatyXowe[i ^ 1][k] - koordynatyXowe[i][j]) * 2.0 / 3.0);
                    wypelnijNaY[j] = (int) (koordynatyYkowe[i][j] + (koordynatyYkowe[i ^ 1][k] - koordynatyYkowe[i][j]) * 2.0 / 3.0);
                    if (dokonywanieLustrzanegoOdbicia)
                        wypelnijNaX[j] = szerokosc - wypelnijNaX[j];
                }
                tekstury.setColor(Color.gray);
                tekstury.fillPolygon(wypelnijNaX, wypelnijNaY, 4);
            } else {


                // draw black face background (do not care about normals and visibility!)
                for (int j = 0; j < 4; j++) // corner co-ordinates
                    pobierzNarozniki(i, j, wypelnijNaX, wypelnijNaY, klocki[i][0][stopnie[j][0]], klocki[i][1][stopnie[j][1]], dokonywanieLustrzanegoOdbicia);
                tekstury.setColor(Color.gray);
                tekstury.fillPolygon(wypelnijNaX, wypelnijNaY, 4);
            }
        }
        // draw all visible faces and get przeciaganie regions
        // sideW1 = bocznaSzerokosc
        // sideH1 = bocznaWysokosc
        for (int a = 0; a < 6; a++) { // all faces
            odejmowanieWektorow(skalowanieWektorow(kopiowanieWektorow(punktWidzenia1, eye), 5.0), wektoryNormalneScianKostki[a]); // perspective correction
            if (tworzenieWektorow(punktWidzenia1, wektoryNormalneScianKostki[a]) > 0) { // draw only faces towards us
                int bocznaSzerokosc = klocki[a][0][1] - klocki[a][0][0];
                int bocznaWysokosc = klocki[a][1][1] - klocki[a][1][0];
                if (bocznaSzerokosc > 0 && bocznaWysokosc > 0) { // this side is not only black
                    // draw colored facelets
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

                // horizontal and vertical directions of face - interpolated
                // dxh1 = kierunekPoziomyNaX
                // dyh1 = kierunekPoziomyNaY
                // dxv1 = kierunekPionowyNaX
                // dyv1 = kierunekPionowyNaY
                // mode1 = tryb
                // areaDirs1 = kierunkiPola
                // twistDirs1 = kierunkiObrotu
                //
                double kierunekPoziomyNaX = (koordynatyXowe[a][1] - koordynatyXowe[a][0] + koordynatyXowe[a][2] - koordynatyXowe[a][3]) / 6.0;
                double kierunekPoziomyNaY = (koordynatyXowe[a][3] - koordynatyXowe[a][0] + koordynatyXowe[a][2] - koordynatyXowe[a][1]) / 6.0;
                double kierunekPionowyNaX = (koordynatyYkowe[a][1] - koordynatyYkowe[a][0] + koordynatyYkowe[a][2] - koordynatyYkowe[a][3]) / 6.0;
                double kierunekPionowyNaY = (koordynatyYkowe[a][3] - koordynatyYkowe[a][0] + koordynatyYkowe[a][2] - koordynatyYkowe[a][1]) / 6.0;
                if (tryb == 3) { // just the normal kostka
                    for (int j = 0; j < 6; j++) { // 4 areas 3x1 per face + 2 center slices
                        for (int k = 0; k < 4; k++) // 4 points per area
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
                } else if (tryb == 0) { // twistable top layer
                    if (a != warstwaObracana && bocznaSzerokosc > 0 && bocznaWysokosc > 0) { // only 3x1 faces
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
                } else if (tryb == 1) { // twistable center layer
                    if (a != warstwaObracana && bocznaSzerokosc > 0 && bocznaWysokosc > 0) { // only 3x1 faces
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


    // face1 = sciana
    // corner1 = naroznik
    // cornersX1 = naroznikWspX
    // cornersY1 = naroznikWspY
    // factor11 = czynnikPierwszy
    // factor21 = czynnikDrugi
    // mirror1 = lustro
    // x11 = pierwszaWspNaX
    // y11 = pierwszaWspNaY
    // x21 = drugaWspNaX
    // y21 = drugaWspNaY
    //
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

    private void tworzeniePrzyciskow(Graphics g) {

        // full buttonbar
        g.setClip(0, wysokosc, szerokosc, wysokoscPrzyciskow);
        int buttonX = 0;
        for (int i = 0; i < 4; i++) {
            int buttonWidth = (szerokosc - buttonX) / (4 - i);
            g.setColor(przyciskKoloruTla);
            g.fill3DRect(buttonX, wysokosc, buttonWidth, wysokoscPrzyciskow, wcisnietyPrzycisk != i);
            //drawButton(g, i, buttonX + buttonWidth / 2, wysokosc + wysokoscPrzyciskow / 2);
            buttonX += buttonWidth;
        }
        tworzeniePrzyciskow = false;
        return;

    }

    /*private void drawButton(Graphics g, int i, int x, int y) {
        g.setColor(Color.white);
        switch (i) {
            case 0: // rewind
                drawRect(g, x - 2, y - 3, 3, 7);
                break;
            case 1: // reverse step
                drawRect(g, x + 2, y - 3, 3, 7);
                drawArrow(g, x, y, -1); // left
                break;
            case 2: // mirror
                drawRect(g, x - 3, y - 2, 7, 5);
                drawRect(g, x - 1, y - 4, 3, 9);
            case 3:
                drawRect(g, x + 3, y - 3, 3, 7);
                break;

        }
    }
*/
   /* private static void drawArrow(Graphics g, int x, int y, int dir) {
        g.setColor(Color.black);
        g.drawLine(x, y - 3, x, y + 3);
        x += dir;
        for (int i = 0; i >= -3 && i <= 3; i += dir) {
            int j = 3 - i * dir;
            g.drawLine(x + i, y + j, x + i, y - j);
        }
        g.setColor(Color.white);
        for (int i = 0; i >= -1 && i <= 1; i += dir) {
            int j = 1 - i * dir;
            g.drawLine(x + i, y + j, x + i, y - j);
        }
    }

    private static void drawRect(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.black);
        g.drawRect(x, y, width - 1, height - 1);
        g.setColor(Color.white);
        g.fillRect(x + 1, y + 1, width - 2, height - 2);
    }

    public static void shuffle(int[][] matrix, int columns, Random random) {
        int size = matrix.length * columns;
        for (int i = size; i > 1; i--)
            swap(matrix, columns, i - 1, random.nextInt(i));
    }

    public static void swap(int[][] matrix, int columns, int i, int j) {
        double tmp = matrix[i / columns][i % columns];
        matrix[i / columns][i % columns] = matrix[j / columns][j % columns];
        matrix[j / columns][j % columns] = (int) tmp;
    }
*/
    //private static final int[] textOffset = {1, 1, -1, -1, -1, 1, 1, -1, -1, 0, 1, 0, 0, 1, 0, -1};


    /*
    private int selectButton(int x, int y) {
        if (PasekPrzyciskow == 0)
            return -1;
        if ( x >= szerokosc - wysokoscPrzyciskow && x < szerokosc && y >= 0 && y < wysokoscPrzyciskow)
            return 3;
        if (PasekPrzyciskow == 2) { // only clear (rewind) button present
            if (x >= 0 && x < wysokoscPrzyciskow && y >= wysokosc - wysokoscPrzyciskow && y < wysokosc)
                return 0;
            return -1;
        }

        if (PasekPrzyciskow == 3){
            return -1;

        }
        if (y < wysokosc)
            return -1;
        int buttonX = 0;
        for (int i = 0; i < 4; i++) {
            int buttonWidth = (szerokosc - buttonX) / (4 - i);
            if (x >= buttonX && x < buttonX + buttonWidth && y >= wysokosc && y < wysokosc + wysokoscPrzyciskow)
                return i;
            buttonX += buttonWidth;
        }
        return -1;
    }
*/






    // Mouse event handlers
     public void mousePressed(MouseEvent e) {
        wcześniejszePolozenieMyszyNaX = wczesniejszaWspolrzednaX = e.getX();
        wcześniejszePolozenieMyszyNaY = wczesniejszaWspolrzednaY = e.getY();
        stanGdzieMoznaObracacWarstwami = false;
        //wcisnietyPrzycisk = selectButton(wczesniejszaWspolrzednaX, wczesniejszaWspolrzednaY);
        if (wcisnietyPrzycisk >= 0) {
            przycisniecie = true;
            if (wcisnietyPrzycisk == 2) {
                dokonywanieLustrzanegoOdbicia = !dokonywanieLustrzanegoOdbicia;
            } else if (wcisnietyPrzycisk == 0) { // clear everything to the initial setup
                clear();
            }
           // else if (wcisnietyPrzycisk == 1){
            //}
            else if (wcisnietyPrzycisk == 3) {
                //shuffle(kostka, 9, new Random(9));
            } else
                tworzeniePrzyciskow = true;
            repaint();
            /**************************************************************************************************************************/
        } else {
            if (zmianaWysokosci > 0 && wczesniejszaWspolrzednaY >= wysokosc - zmianaWysokosci && wczesniejszaWspolrzednaY < wysokosc) {

                repaint();
            } else {
                if (dokonywanieLustrzanegoOdbicia)
                    wcześniejszePolozenieMyszyNaX = wczesniejszaWspolrzednaX = szerokosc - wczesniejszaWspolrzednaX;
                if (dokonywanieZmianNaKostce &&
                        (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                        (e.getModifiers() & InputEvent.SHIFT_MASK) == 0)
                    stanGdzieMoznaObracacWarstwami = true;
            }
        }
    }


    // angle = kat
    // num = liczba
    public void mouseReleased(MouseEvent e) {
        przeciaganie = false;
        if (przycisniecie) {
            przycisniecie = false;
            tworzeniePrzyciskow = true;
            repaint();
        } else if (dokonywanieObrotuWarstwy) {
            dokonywanieObrotuWarstwy = false;
            katPrzedObrotem += katPoObrocie;
            katPoObrocie = 0.0;
            double kat = katPrzedObrotem;
            while (kat < 0.0)
                kat += 32.0 * Math.PI;
            int liczba = (int) (kat * 8.0 / Math.PI) % 16; // 2pi ~ 16
            if (liczba % 4 == 0 || liczba % 4 == 3) { // close enough to a corner
                liczba = (liczba + 1) / 4; // 2pi ~ 4
                if (kierunkiScianPrzyObracaniu[warstwaObracana] > 0)
                    liczba = (4 - liczba) % 4;
                katPrzedObrotem = 0;
                stanObojetny = true; // the kostka in the stanObojetny state
                obracanieWarstw(kostka, warstwaObracana, liczba, obracanie); // rotate the facelets
            }
            repaint();
        }
    }

    private final double[] eyeD = new double[3];




    // x1 = wspXObecna
    // y1 = wspYObecna
    // dx1 = wspXPrzed
    // dy1 = wspYPrzed
    // d11 = zmiennaKierunkowa1
    // d21 = zmiennaKierunkowa2
    // x2 = drugaWspX
    // y2 = drugaWspY
    public void mouseDragged(MouseEvent e) {



        //if (przycisniecie)
        //    return;


        int polozenieX = dokonywanieLustrzanegoOdbicia ? szerokosc - e.getX() : e.getX();
        int polozenieY = e.getY();
        int wspXPrzed = polozenieX - wczesniejszaWspolrzednaX;
        int wspYPrzed = polozenieY - wczesniejszaWspolrzednaY;



        if (dokonywanieZmianNaKostce && stanGdzieMoznaObracacWarstwami && !dokonywanieObrotuWarstwy) { // we do not twist but we can
            wcześniejszePolozenieMyszyNaX = polozenieX;
            wcześniejszePolozenieMyszyNaY = polozenieY;
            for (int i = 0; i < obszarPrzeciaganiaMysza; i++) { // check if inside a drag area
                double zmiennaKierunkowa1 = przeciaganieNaroznikowNaX[i][0];
                double wspXObecna = przeciaganieNaroznikowNaX[i][1] - zmiennaKierunkowa1;
                double wspYObecna = przeciaganieNaroznikowNaX[i][3] - zmiennaKierunkowa1;
                double zmiennaKierunkowa2 = przeciaganieNaroznikowNaY[i][0];
                double drugaWspX = przeciaganieNaroznikowNaY[i][1] - zmiennaKierunkowa2;
                double drugaWspY = przeciaganieNaroznikowNaY[i][3] - zmiennaKierunkowa2;
                double a = (drugaWspY * (wczesniejszaWspolrzednaX - zmiennaKierunkowa1) - wspYObecna * (wczesniejszaWspolrzednaY - zmiennaKierunkowa2)) / (wspXObecna * drugaWspY - wspYObecna * drugaWspX);
                double b = (-drugaWspX * (wczesniejszaWspolrzednaX - zmiennaKierunkowa1) + wspXObecna * (wczesniejszaWspolrzednaY - zmiennaKierunkowa2)) / (wspXObecna * drugaWspY - wspYObecna * drugaWspX);
                if (a > 0 && a < 1 && b > 0 && b < 1) { // we are in
                    if (wspXPrzed * wspXPrzed + wspYPrzed * wspYPrzed < 144) // delay the decision about dokonywanieObrotuWarstwy
                        return;
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


        if (!dokonywanieObrotuWarstwy) { // whole kostka rotation
            wektorNormalny(vAdd(obserwator, skalowanieWektorow(kopiowanieWektorow(eyeD, widokBokiem), wspXPrzed * -0.0016)));
            wektorNormalny(mnozenieWektorow(widokBokiem, widokPionowy, obserwator));
            wektorNormalny(vAdd(obserwator, skalowanieWektorow(kopiowanieWektorow(eyeD, widokPionowy), wspYPrzed * 0.0016)));
            wektorNormalny(mnozenieWektorow(widokPionowy, obserwator, widokBokiem));
            wczesniejszaWspolrzednaX = polozenieX;
            wczesniejszaWspolrzednaY = polozenieY;
        }

        else {
            if (stanObojetny)
                warstwyKostki(warstwaObracana);
            katPoObrocie = 0.005 * (przeciaganiePoX * wspXPrzed + przeciaganiePoY * wspYPrzed) / Math.sqrt(przeciaganiePoX * przeciaganiePoX + przeciaganiePoY * przeciaganiePoY); // dv * cos a
        }


        repaint();
    }




    // status bar help strings
    private static final String[] buttonDescriptions = {
            "Stan początkowy",
            "Cofnij ruch",
            "Odbicie lustrzane",
            "Tasuj",

    };
    private String buttonDescription = "";

    public void mouseMoved(MouseEvent e) {
        int a = e.getX();
        int b = e.getY();
        String description = "Przesuń kostkę myszką";
        if (a >= 0 && a < szerokosc) {
            if (b >= wysokosc && b < wysokosc + wysokoscPrzyciskow || b >= 0 && b < wysokoscPrzyciskow) {
                //wcisnietyPrzycisk = selectButton(a, b);
                if (wcisnietyPrzycisk >= 0)
                    description = buttonDescriptions[wcisnietyPrzycisk];

            }
        }
        if (description != buttonDescription) {
            buttonDescription = description;
            showStatus(description);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    // Various useful vector functions

    private static double[] kopiowanieWektorow(double[] vector, double[] srcVec) {
        vector[0] = srcVec[0];
        vector[1] = srcVec[1];
        vector[2] = srcVec[2];
        return vector;
    }


    // vector1 = wektor
    // vNorm = wektorNormalny
    private static double[] wektorNormalny(double[] wektor) {
        double length = Math.sqrt(tworzenieWektorow(wektor, wektor));
        wektor[0] /= length;
        wektor[1] /= length;
        wektor[2] /= length;
        return wektor;
    }


    // value1 = liczba
    private static double[] skalowanieWektorow(double[] wektor, double liczba) {
        wektor[0] *= liczba;
        wektor[1] *= liczba;
        wektor[2] *= liczba;
        return wektor;
    }


    // wektor1 = wektor1
    // wektor2 = wektor2
    private static double tworzenieWektorow(double[] wektor1, double[] wektor2) {
        return wektor1[0] * wektor2[0] + wektor1[1] * wektor2[1] + wektor1[2] * wektor2[2];
    }


    // srcVec1 = wektorOryginalny
    private static double[] vAdd(double[] wektor, double[] wektorOryginalny) {
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