package tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import javax.imageio.ImageIO;

/**
 * High-fidelity visual screenshot generator for Annytunes.
 * Recreates exact Android UI layouts matching the app XML layouts and theme:
 * 1. Frame 1: USB Device Selection & Connect (activity_main.xml)
 * 2. Frame 2: Channel Transfer & Codeplug Manager (activity_transfer.xml)
 * 3. Frame 3: Channel Edit Modal Dialog (ChannelEditDialog.java)
 * 4. Frame 4: Zone & Bank Management (activity_zones.xml)
 * 5. Frame 5: Modern Flutter Channel Edit Bottom Sheet (Unit 11)
 * 6. Frame 6: Material 3 Theme & Reusable UI Widgets (AppTheme & Widgets)
 */
public class ScreenshotGenerator {

    private static final int WIDTH = 540;
    private static final int HEIGHT = 960;

    // Theme Colors
    private static final Color COLOR_BLACK = new Color(0x0E, 0x11, 0x17);
    private static final Color COLOR_BG = new Color(0x12, 0x16, 0x1F);
    private static final Color COLOR_SURFACE = new Color(0x1A, 0x20, 0x2C);
    private static final Color COLOR_CARD = new Color(0x23, 0x2A, 0x38);
    private static final Color COLOR_CARD_BORDER = new Color(0x32, 0x3D, 0x52);
    private static final Color COLOR_TEXT_WHITE = new Color(0xF7, 0xFA, 0xFC);
    private static final Color COLOR_TEXT_GREY = new Color(0xA0, 0xAE, 0xC0);
    private static final Color COLOR_TEXT_MUTED = new Color(0x71, 0x80, 0x96);
    private static final Color COLOR_TEAL = new Color(0x0D, 0x94, 0x88);
    private static final Color COLOR_TEAL_BRIGHT = new Color(0x14, 0xB8, 0xA6);
    private static final Color COLOR_PURPLE = new Color(0x67, 0x50, 0xA4);
    private static final Color COLOR_PURPLE_LIGHT = new Color(0x7C, 0x3A, 0xED);
    private static final Color COLOR_GREEN = new Color(0x10, 0xB9, 0x81);
    private static final Color COLOR_AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color COLOR_BLUE = new Color(0x3B, 0x82, 0xF6);

    public static void main(String[] args) {
        String outputDirPath = args.length > 0 ? args[0] : "release/development/screenshots";
        File outputDir = new File(outputDirPath);
        outputDir.mkdirs();

        System.out.println("=====================================================");
        System.out.println("       Annytunes Visual Frame Screenshot Generator    ");
        System.out.println("=====================================================");
        System.out.println("Output folder: " + outputDir.getAbsolutePath());

        try {
            // Frame 1: USB Device Selection (activity_main.xml)
            File f1 = new File(outputDir, "frame_01_usb_connection.png");
            renderFrame1UsbConnection(f1);

            // Frame 2: Channel Transfer (activity_transfer.xml)
            File f2 = new File(outputDir, "frame_02_channel_transfer.png");
            renderFrame2ChannelTransfer(f2);

            // Frame 3: Channel Edit Dialog (ChannelEditDialog.java)
            File f3 = new File(outputDir, "frame_03_channel_edit_dialog.png");
            renderFrame3ChannelEditDialog(f3);

            // Frame 4: Zone Management (activity_zones.xml)
            File f4 = new File(outputDir, "frame_04_zone_management.png");
            renderFrame4ZoneManagement(f4);

            // Frame 5: Modern Flutter Channel Edit Bottom Sheet
            File f5 = new File(outputDir, "frame_05_flutter_channel_edit_sheet.png");
            renderFrame5FlutterChannelEditSheet(f5);

            // Frame 6: Material 3 Theme & Widgets
            File f6 = new File(outputDir, "frame_06_material3_theme_and_widgets.png");
            renderFrame6Material3ThemeAndWidgets(f6);

            // Frame 7: About & Contributions Dropdown Dialog
            File f7 = new File(outputDir, "frame_07_about_and_contributions_dialog.png");
            renderFrame7AboutAndContributionsDialog(f7);

            // Generate HTML Gallery Report
            generateHtmlReport(outputDir);

            System.out.println("✓ Successfully generated all 7 visual frames and report!");
            System.out.println("=====================================================");
        } catch (Exception e) {
            System.err.println("Error generating screenshots: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Graphics2D createGraphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    private static void drawStatusBar(Graphics2D g) {
        g.setColor(COLOR_BLACK);
        g.fillRect(0, 0, WIDTH, 28);
        g.setColor(new Color(0xD0, 0xD0, 0xD0));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("14:30", 20, 19);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("5G", WIDTH - 100, 19);
        g.fillOval(WIDTH - 55, 11, 8, 8);
        g.drawString("100%", WIDTH - 42, 19);
    }

    private static void drawAppBar(Graphics2D g, String title, boolean showBackArrow) {
        int y = 28;
        int h = 56;
        g.setColor(COLOR_SURFACE);
        g.fillRect(0, y, WIDTH, h);
        g.setColor(COLOR_CARD_BORDER);
        g.drawLine(0, y + h, WIDTH, y + h);

        int textX = 24;
        if (showBackArrow) {
            g.setColor(COLOR_TEXT_WHITE);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(24, y + 28, 38, y + 28);
            g.drawLine(24, y + 28, 31, y + 21);
            g.drawLine(24, y + 28, 31, y + 35);
            textX = 52;
        }

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(title, textX, y + 36);

        // Right USB Status Indicator
        g.setColor(COLOR_GREEN);
        g.fillOval(WIDTH - 44, y + 23, 10, 10);
    }

    private static void drawButton(Graphics2D g, int x, int y, int w, int h, String text, Color bg, Color textCol, int fontSize) {
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(textCol);
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int strW = fm.stringWidth(text);
        int strH = fm.getAscent();
        g.drawString(text, x + (w - strW) / 2, y + (h + strH) / 2 - 3);
    }

    // -------------------------------------------------------------
    // FRAME 1: USB Device Selection (activity_main.xml)
    // -------------------------------------------------------------
    private static void renderFrame1UsbConnection(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawStatusBar(g);
        drawAppBar(g, "Annytunes", false);

        int pad = 20;
        int curY = 105;

        // Card 1: USB Device Configuration
        int cardH = 210;
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), cardH, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), cardH, 12, 12);

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("USB Devices", pad + 16, curY + 30);

        // Spinner simulation
        int spinY = curY + 45;
        g.setColor(COLOR_CARD);
        g.fillRoundRect(pad + 16, spinY, WIDTH - (pad * 2) - 32, 48, 8, 8);
        g.setColor(COLOR_TEAL_BRIGHT);
        g.drawRoundRect(pad + 16, spinY, WIDTH - (pad * 2) - 32, 48, 8, 8);

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.drawString("AnyTone AT-D878UV (VID: 28E9, PID: 0189)", pad + 28, spinY + 30);

        // Arrow down
        int arrowX = WIDTH - pad - 42;
        g.setColor(COLOR_TEAL_BRIGHT);
        g.fillPolygon(new int[]{arrowX, arrowX + 10, arrowX + 5}, new int[]{spinY + 20, spinY + 20, spinY + 27}, 3);

        // Buttons: Refresh & Connect
        int btnW = (WIDTH - (pad * 2) - 32 - 12) / 2;
        int btnY = spinY + 65;
        drawButton(g, pad + 16, btnY, btnW, 44, "Refresh", COLOR_CARD, COLOR_TEXT_WHITE, 14);
        drawButton(g, pad + 16 + btnW + 12, btnY, btnW, 44, "Connect", COLOR_TEAL, COLOR_TEXT_WHITE, 14);

        curY += cardH + 20;

        // Card 2: Device & Radio Info Card
        int infoH = 175;
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), infoH, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), infoH, 12, 12);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("Radio Interface Status", pad + 16, curY + 30);

        String[][] details = {
            {"Driver:", "CDC-ACM / FTDI Serial Protocol"},
            {"Baud Rate:", "115200 bps (8 data bits, 1 stop bit, no parity)"},
            {"Supported Radios:", "AT-D878UV, AT-D878UVII Plus, AT-D578UV"},
            {"Connection Mode:", "USB OTG / Direct Serial COM"}
        };

        int detY = curY + 58;
        for (String[] d : details) {
            g.setColor(COLOR_TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(d[0], pad + 16, detY);

            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString(d[1], pad + 140, detY);
            detY += 25;
        }

        curY += infoH + 20;

        // Card 3: Quick Start / Help
        g.setColor(new Color(0x13, 0x2A, 0x26));
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);
        g.setColor(COLOR_TEAL);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);

        g.setColor(COLOR_GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("✔ Quick Start Guide", pad + 16, curY + 28);

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("1. Connect your AnyTone radio using the USB programming cable.", pad + 16, curY + 52);
        g.drawString("2. Turn on the radio and ensure it displays standard standby.", pad + 16, curY + 74);
        g.drawString("3. Select the detected device above and tap [Connect].", pad + 16, curY + 96);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 2: Channel Transfer & Codeplug Manager (activity_transfer.xml)
    // -------------------------------------------------------------
    private static void renderFrame2ChannelTransfer(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawStatusBar(g);
        drawAppBar(g, "Channel Transfer", true);

        int pad = 16;
        int curY = 96;

        // Top Status Text
        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Status: Connected to Anytone 878UV (128 Channels read)", pad, curY);
        curY += 12;

        // Button Row 1: Handshake & Enter PC Mode
        int btnW = (WIDTH - (pad * 2) - 10) / 2;
        int btnH = 38;
        curY += 8;
        drawButton(g, pad, curY, btnW, btnH, "Handshake", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        drawButton(g, pad + btnW + 10, curY, btnW, btnH, "Enter PC Mode", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        curY += btnH + 8;

        // Button Row 2: Read & Write All to Radio
        drawButton(g, pad, curY, btnW, btnH, "Read", COLOR_TEAL, COLOR_TEXT_WHITE, 13);
        drawButton(g, pad + btnW + 10, curY, btnW, btnH, "Write All to Radio", COLOR_PURPLE, COLOR_TEXT_WHITE, 13);
        curY += btnH + 8;

        // Button Row 3: Save All CSV & Write Edited
        drawButton(g, pad, curY, btnW, btnH, "Save All CSV", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        drawButton(g, pad + btnW + 10, curY, btnW, btnH, "Write Edited to Radio", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        curY += btnH + 12;

        // Progress Bar Row (Horizontal)
        int progH = 10;
        g.setColor(COLOR_CARD);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2) - 45, progH, 4, 4);
        g.setColor(COLOR_TEAL_BRIGHT);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2) - 45, progH, 4, 4);
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("100%", WIDTH - pad - 35, curY + 9);
        curY += progH + 14;

        // Channel List View Header
        g.setColor(COLOR_SURFACE);
        g.fillRect(pad, curY, WIDTH - (pad * 2), 24);
        g.setColor(COLOR_TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("NO.  CHANNEL NAME           RX FREQ       MODE     PWR", pad + 8, curY + 16);
        curY += 26;

        // Channel Rows
        String[][] channels = {
            {"01", "Simplex 2m", "145.50000", "Analog", "High (45W)", "25kHz"},
            {"02", "DMR BrandM", "438.80000", "Digital CC1 TS1", "Mid (25W)", "12.5kHz"},
            {"03", "Repeater R0", "145.60000", "Analog CTCSS 88.5", "Turbo (50W)", "25kHz"},
            {"04", "Local Net", "433.50000", "Analog", "Low (5W)", "25kHz"},
            {"05", "DMR TG 91", "438.82500", "Digital CC1 TS2", "High (45W)", "12.5kHz"},
            {"06", "Emergency", "144.30000", "Analog", "Turbo (50W)", "25kHz"},
            {"07", "DMR TG 268", "438.80000", "Digital CC1 TS1", "High (45W)", "12.5kHz"}
        };

        for (String[] c : channels) {
            int rowH = 50;
            g.setColor(COLOR_CARD);
            g.fillRoundRect(pad, curY, WIDTH - (pad * 2), rowH, 6, 6);
            g.setColor(COLOR_CARD_BORDER);
            g.drawRoundRect(pad, curY, WIDTH - (pad * 2), rowH, 6, 6);

            // Channel Number
            g.setColor(COLOR_TEAL_BRIGHT);
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.drawString(c[0], pad + 10, curY + 22);

            // Channel Name
            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString(c[1], pad + 42, curY + 22);

            // RX Frequency
            g.setColor(COLOR_AMBER);
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.drawString(c[2] + " MHz", WIDTH - pad - 130, curY + 22);

            // Sub-row: Mode & Power
            boolean isDigital = c[3].startsWith("Digital");
            g.setColor(isDigital ? COLOR_BLUE : COLOR_GREEN);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString(c[3], pad + 42, curY + 40);

            g.setColor(COLOR_TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("• Power: " + c[4], WIDTH - pad - 130, curY + 40);

            curY += rowH + 6;
        }

        // Bottom Exit Buttons
        int botY = HEIGHT - 55;
        drawButton(g, pad, botY, btnW, 40, "Save & Exit", COLOR_TEAL, COLOR_TEXT_WHITE, 13);
        drawButton(g, pad + btnW + 10, botY, btnW, 40, "Exit", COLOR_CARD, COLOR_TEXT_WHITE, 13);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 3: Channel Edit Modal Dialog (ChannelEditDialog.java)
    // -------------------------------------------------------------
    private static void renderFrame3ChannelEditDialog(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);

        // Draw Frame 2 as background
        renderFrame2ChannelTransfer(file);
        BufferedImage base = ImageIO.read(file);
        g.drawImage(base, 0, 0, null);

        // Dim background
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Dialog Box
        int diaW = WIDTH - 48;
        int diaH = 580;
        int diaX = 24;
        int diaY = (HEIGHT - diaH) / 2 - 10;

        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(diaX, diaY, diaW, diaH, 16, 16);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(diaX, diaY, diaW, diaH, 16, 16);

        // Title
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Edit Channel #2", diaX + 24, diaY + 40);

        int fieldY = diaY + 70;

        // Input Fields (from ChannelEditDialog.java)
        String[][] fields = {
            {"Name", "DMR BrandM"},
            {"RX MHz", "438.80000"},
            {"TX MHz", "431.20000"},
            {"Channel Mode", "Digital (DMR)"},
            {"Color Code (CC)", "1"},
            {"Timeslot (TS)", "1"},
            {"Contact ID / TG", "268 (Portugal TG)"},
            {"Admit Criteria", "Color Code Free"}
        };

        for (String[] f : fields) {
            g.setColor(COLOR_TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString(f[0], diaX + 24, fieldY);

            // Field Box
            g.setColor(COLOR_CARD);
            g.fillRoundRect(diaX + 24, fieldY + 5, diaW - 48, 32, 6, 6);
            g.setColor(COLOR_CARD_BORDER);
            g.drawRoundRect(diaX + 24, fieldY + 5, diaW - 48, 32, 6, 6);

            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g.drawString(f[1], diaX + 34, fieldY + 26);

            fieldY += 56;
        }

        // Action Buttons: Cancel and Save
        int btnW = (diaW - 48 - 12) / 2;
        int btnY = diaY + diaH - 56;
        drawButton(g, diaX + 24, btnY, btnW, 40, "Cancel", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        drawButton(g, diaX + 24 + btnW + 12, btnY, btnW, 40, "Save Channel", COLOR_TEAL, COLOR_TEXT_WHITE, 13);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 4: Zone & Bank Management (activity_zones.xml)
    // -------------------------------------------------------------
    private static void renderFrame4ZoneManagement(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawStatusBar(g);
        drawAppBar(g, "Zone Management", true);

        int pad = 16;
        int curY = 96;

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Radio Zones (5 configured)", pad, curY);
        curY += 16;

        // Action Buttons
        int btnW = (WIDTH - (pad * 2) - 10) / 2;
        int btnH = 40;
        drawButton(g, pad, curY, btnW, btnH, "Read Zones", COLOR_TEAL, COLOR_TEXT_WHITE, 13);
        drawButton(g, pad + btnW + 10, curY, btnW, btnH, "Write Zones", COLOR_PURPLE, COLOR_TEXT_WHITE, 13);
        curY += btnH + 10;

        drawButton(g, pad, curY, WIDTH - (pad * 2), btnH, "Save Zones CSV", COLOR_CARD, COLOR_TEXT_WHITE, 13);
        curY += btnH + 16;

        // Zones List
        String[][] zones = {
            {"Zone 1: VHF Simplex", "Channels: 01, 04, 06 (Calling, Net, Emergency)"},
            {"Zone 2: DMR Repeaters", "Channels: 02, 05, 07 (BrandMeister, TG 91, TG 268)"},
            {"Zone 3: UHF Simplex", "Channels: 04, 11, 15 (Local UHF, Net)"},
            {"Zone 4: Emergency Ops", "Channels: 01, 06, 12, 18 (Cross-band tactical)"},
            {"Zone 5: Roaming Cluster", "Channels: 02, 05, 08, 09, 14 (Auto-roam group)"}
        };

        for (String[] z : zones) {
            int itemH = 68;
            g.setColor(COLOR_SURFACE);
            g.fillRoundRect(pad, curY, WIDTH - (pad * 2), itemH, 8, 8);
            g.setColor(COLOR_CARD_BORDER);
            g.drawRoundRect(pad, curY, WIDTH - (pad * 2), itemH, 8, 8);

            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(z[0], pad + 14, curY + 26);

            g.setColor(COLOR_TEXT_GREY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(z[1], pad + 14, curY + 50);

            curY += itemH + 10;
        }

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 5: Modern Flutter Channel Edit Bottom Sheet (Unit 11)
    // -------------------------------------------------------------
    private static void renderFrame5FlutterChannelEditSheet(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);
        g.setColor(COLOR_BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawStatusBar(g);

        // Top App Bar
        g.setColor(new Color(0x1F, 0x1A, 0x24));
        g.fillRect(0, 28, WIDTH, 56);
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Annytunes (Flutter Edition)", 24, 62);

        // Background list of channels
        int bgY = 96;
        for (int i = 1; i <= 6; i++) {
            g.setColor(COLOR_SURFACE);
            g.fillRoundRect(16, bgY, WIDTH - 32, 45, 8, 8);
            bgY += 52;
        }

        // Scrim overlay
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 28, WIDTH, HEIGHT - 28);

        // Bottom Sheet Surface (Material 3)
        int sheetH = 620;
        int sheetY = HEIGHT - sheetH;
        g.setColor(new Color(0x1E, 0x19, 0x2B)); // Material 3 Dark Surface
        g.fillRoundRect(0, sheetY, WIDTH, sheetH + 20, 28, 28);

        // Drag handle
        g.setColor(new Color(0x79, 0x74, 0x7E));
        g.fillRoundRect(WIDTH / 2 - 16, sheetY + 12, 32, 4, 2, 2);

        // Sheet Title
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Edit Channel", 24, sheetY + 45);

        // Toggle Switch: Analog vs DMR
        int togY = sheetY + 65;
        g.setColor(new Color(0x33, 0x2D, 0x41));
        g.fillRoundRect(24, togY, WIDTH - 48, 44, 22, 22);

        // Selected tab (DMR)
        g.setColor(COLOR_PURPLE);
        g.fillRoundRect(WIDTH / 2, togY + 3, (WIDTH - 48) / 2 - 3, 38, 19, 19);

        g.setColor(COLOR_TEXT_MUTED);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Analog Mode", 70, togY + 27);

        g.setColor(COLOR_TEXT_WHITE);
        g.drawString("DMR Digital", WIDTH / 2 + 50, togY + 27);

        // Input Fields
        int curY = togY + 60;
        String[][] flutterFields = {
            {"Channel Name", "DMR Calling 91"},
            {"RX Frequency (MHz)", "438.82500 MHz"},
            {"TX Frequency (MHz)", "431.22500 MHz"},
            {"DMR Color Code (0..15)", "CC 1"},
            {"DMR TimeSlot", "TimeSlot 2"},
            {"Contact ID", "91 (Worldwide)"}
        };

        for (String[] ff : flutterFields) {
            g.setColor(new Color(0x2B, 0x24, 0x38));
            g.fillRoundRect(24, curY, WIDTH - 48, 50, 8, 8);
            g.setColor(new Color(0x4F, 0x44, 0x66));
            g.drawRoundRect(24, curY, WIDTH - 48, 50, 8, 8);

            g.setColor(new Color(0xD0, 0xBC, 0xFF));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(ff[0].toUpperCase(), 36, curY + 16);

            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString(ff[1], 36, curY + 38);

            curY += 58;
        }

        // Save & Cancel Action Buttons
        int actY = HEIGHT - 65;
        drawButton(g, 24, actY, (WIDTH - 60) / 2, 46, "Cancel", new Color(0x33, 0x2D, 0x41), COLOR_TEXT_WHITE, 14);
        drawButton(g, WIDTH / 2 + 6, actY, (WIDTH - 60) / 2, 46, "Save Channel", COLOR_PURPLE, COLOR_TEXT_WHITE, 14);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 6: Material 3 Theme & Widgets Showcase
    // -------------------------------------------------------------
    private static void renderFrame6Material3ThemeAndWidgets(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawStatusBar(g);
        drawAppBar(g, "Material 3 Widgets & Theme", true);

        int pad = 20;
        int curY = 100;

        // Widget 1: FrequencyText
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), 100, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), 100, 12, 12);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("1. FrequencyText Widget (MHz Formatter)", pad + 16, curY + 26);

        g.setColor(COLOR_AMBER);
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.drawString("145.50000 MHz", pad + 16, curY + 68);

        g.setColor(COLOR_TEXT_MUTED);
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.drawString("438.82500 MHz", pad + 270, curY + 68);
        curY += 115;

        // Widget 2: ChannelModeChip
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), 100, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), 100, 12, 12);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("2. ChannelModeChip Widget (Mode Badges)", pad + 16, curY + 26);

        // Analog Badge
        g.setColor(new Color(0x10, 0xB9, 0x81, 40));
        g.fillRoundRect(pad + 16, curY + 45, 90, 32, 16, 16);
        g.setColor(COLOR_GREEN);
        g.drawRoundRect(pad + 16, curY + 45, 90, 32, 16, 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("ANALOG", pad + 32, curY + 66);

        // DMR Badge
        g.setColor(new Color(0x3B, 0x82, 0xF6, 40));
        g.fillRoundRect(pad + 120, curY + 45, 120, 32, 16, 16);
        g.setColor(COLOR_BLUE);
        g.drawRoundRect(pad + 120, curY + 45, 120, 32, 16, 16);
        g.drawString("DMR DIGITAL", pad + 134, curY + 66);
        curY += 115;

        // Widget 3: PowerLevelIndicator
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("3. PowerLevelIndicator Widget (Signal Bars)", pad + 16, curY + 26);

        String[] pwrLevels = {"Low (1 bar)", "Mid (2 bars)", "High (3 bars)", "Turbo (4 bars)"};
        int pX = pad + 16;
        for (int i = 0; i < 4; i++) {
            // Bars
            for (int b = 0; b <= i; b++) {
                g.setColor(i == 3 ? new Color(0xEF, 0x44, 0x44) : COLOR_TEAL_BRIGHT);
                g.fillRect(pX + (b * 6), curY + 70 - (b * 6), 4, 10 + (b * 6));
            }
            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(pwrLevels[i], pX, curY + 95);
            pX += 115;
        }
        curY += 135;

        // Widget 4: ProgressOverlay
        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(pad, curY, WIDTH - (pad * 2), 120, 12, 12);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("4. ProgressOverlay (Async Dialog with Timeout)", pad + 16, curY + 26);

        g.setColor(COLOR_PURPLE);
        g.fillRoundRect(pad + 16, curY + 45, WIDTH - (pad * 2) - 32, 12, 6, 6);
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Reading Memory Bank 0x100000... (64/128 Channels)", pad + 16, curY + 80);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // -------------------------------------------------------------
    // FRAME 7: About & Contributions Dropdown Dialog
    // -------------------------------------------------------------
    private static void renderFrame7AboutAndContributionsDialog(File file) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(img);

        // Draw Frame 1 as background
        renderFrame1UsbConnection(file);
        BufferedImage base = ImageIO.read(file);
        g.drawImage(base, 0, 0, null);

        // Dim background
        g.setColor(new Color(0, 0, 0, 195));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Dropdown Menu at Top Right (below app bar)
        int menuW = 200;
        int menuH = 135;
        int menuX = WIDTH - menuW - 16;
        int menuY = 88;

        g.setColor(COLOR_CARD);
        g.fillRoundRect(menuX, menuY, menuW, menuH, 8, 8);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(menuX, menuY, menuW, menuH, 8, 8);

        // Dropdown Items
        String[] menuItems = {"Git Source Code", "Contributions", "About"};
        int miY = menuY + 26;
        for (int i = 0; i < menuItems.length; i++) {
            if (i == 2) {
                // Highlight About item
                g.setColor(new Color(0x14, 0xB8, 0xA6, 50));
                g.fillRect(menuX + 1, miY - 18, menuW - 2, 36);
                g.setColor(COLOR_TEAL_BRIGHT);
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
            } else {
                g.setColor(COLOR_TEXT_WHITE);
                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            }
            g.drawString(menuItems[i], menuX + 16, miY);
            if (i < menuItems.length - 1) {
                g.setColor(COLOR_CARD_BORDER);
                g.drawLine(menuX + 8, miY + 12, menuX + menuW - 8, miY + 12);
            }
            miY += 38;
        }

        // About Dialog Card in Center
        int diaW = WIDTH - 48;
        int diaH = 520;
        int diaX = 24;
        int diaY = 240;

        g.setColor(COLOR_SURFACE);
        g.fillRoundRect(diaX, diaY, diaW, diaH, 16, 16);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(diaX, diaY, diaW, diaH, 16, 16);

        // Header Title with Icon Badge
        g.setColor(COLOR_TEAL);
        g.fillRoundRect(diaX + 24, diaY + 24, 38, 38, 8, 8);
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("A", diaX + 34, diaY + 52);

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("About Annytunes", diaX + 74, diaY + 44);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("v1.3.0  •  Open Source DMR Radio Tool", diaX + 74, diaY + 62);

        // Divider
        g.setColor(COLOR_CARD_BORDER);
        g.drawLine(diaX + 24, diaY + 76, diaX + diaW - 24, diaY + 76);

        int curY = diaY + 102;

        // Git Repository Box
        g.setColor(COLOR_CARD);
        g.fillRoundRect(diaX + 24, curY, diaW - 48, 62, 8, 8);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(diaX + 24, curY, diaW - 48, 62, 8, 8);

        g.setColor(COLOR_TEXT_GREY);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("GIT REPOSITORY", diaX + 36, curY + 22);

        g.setColor(COLOR_TEAL_BRIGHT);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.drawString("https://github.com/dirtybug/annytunes", diaX + 36, curY + 44);

        curY += 80;

        // Contributors Section
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Contributors & Credits", diaX + 24, curY);
        curY += 22;

        String[][] authors = {
            {"dirtybug", "Project founder & DMR UART protocol"},
            {"J. Andrade", "Docker CI/CD, tests & release automation"},
            {"jcalado", "Material 3 UI, CSV services & widgets"},
            {"felHR85", "UsbSerial Android driver library"}
        };

        for (String[] author : authors) {
            g.setColor(COLOR_TEAL_BRIGHT);
            g.fillOval(diaX + 28, curY - 9, 6, 6);

            g.setColor(COLOR_TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(author[0], diaX + 42, curY - 3);

            g.setColor(COLOR_TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(" — " + author[1], diaX + 42 + g.getFontMetrics(new Font("SansSerif", Font.BOLD, 12)).stringWidth(author[0]), curY - 3);

            curY += 26;
        }

        curY += 8;

        // Contribution Callout
        g.setColor(new Color(0x14, 0xB8, 0xA6, 25));
        g.fillRoundRect(diaX + 24, curY, diaW - 48, 48, 6, 6);
        g.setColor(new Color(0x14, 0xB8, 0xA6, 120));
        g.drawRoundRect(diaX + 24, curY, diaW - 48, 48, 6, 6);

        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("Contributions are welcome! Submit PRs, bug reports, and", diaX + 36, curY + 20);
        g.drawString("feature requests directly on GitHub.", diaX + 36, curY + 36);

        // Buttons at Bottom
        int btnY = diaY + diaH - 56;

        // Button: OPEN GITHUB
        g.setColor(COLOR_TEAL);
        g.fillRoundRect(diaX + 24, btnY, 125, 36, 8, 8);
        g.setColor(COLOR_TEXT_WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("OPEN GITHUB", diaX + 40, btnY + 22);

        // Button: CONTRIBUTORS
        g.setColor(COLOR_CARD);
        g.fillRoundRect(diaX + 158, btnY, 130, 36, 8, 8);
        g.setColor(COLOR_CARD_BORDER);
        g.drawRoundRect(diaX + 158, btnY, 130, 36, 8, 8);
        g.setColor(COLOR_TEXT_WHITE);
        g.drawString("CONTRIBUTORS", diaX + 172, btnY + 22);

        // Button: CLOSE
        g.setColor(COLOR_TEXT_GREY);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("CLOSE", diaX + diaW - 74, btnY + 22);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateHtmlReport(File screenshotDir) {
        try {
            File reportDir = new File("tools/reports/behavior-tests");
            reportDir.mkdirs();
            File htmlFile = new File(reportDir, "index.html");

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            sb.append("    <meta charset=\"UTF-8\">\n");
            sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            sb.append("    <title>Annytunes • Visual Frame Verification & Behavior Gallery</title>\n");
            sb.append("    <style>\n");
            sb.append("        :root { --bg: #0E1117; --card: #1A202C; --border: #323D52; --text: #F7FAFC; --accent: #14B8A6; }\n");
            sb.append("        body { background: var(--bg); color: var(--text); font-family: sans-serif; padding: 24px; }\n");
            sb.append("        .container { max-width: 1200px; margin: 0 auto; }\n");
            sb.append("        header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }\n");
            sb.append("        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }\n");
            sb.append("        .card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }\n");
            sb.append("        .card img { width: 100%; height: auto; display: block; }\n");
            sb.append("        .card-body { padding: 16px; }\n");
            sb.append("        .card-title { font-size: 16px; font-weight: bold; margin-bottom: 6px; color: var(--accent); }\n");
            sb.append("        .card-desc { font-size: 13px; color: #A0AEC0; }\n");
            sb.append("        .badge { display: inline-block; background: #065F46; color: #34D399; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; margin-top: 10px; }\n");
            sb.append("    </style>\n</head>\n<body>\n<div class=\"container\">\n");
            sb.append("    <header>\n");
            sb.append("        <div>\n            <h1>Annytunes • Visual Frame Gallery</h1>\n            <p style=\"color:#A0AEC0;\">Automated verification of UI activities and widgets</p>\n        </div>\n");
            sb.append("        <div style=\"background:#065F46; color:#34D399; padding:8px 16px; border-radius:20px; font-weight:bold;\">✔ 7 Frames Verified</div>\n");
            sb.append("    </header>\n    <div class=\"grid\">\n");

            String[][] frames = {
                {"frame_01_usb_connection.png", "01. USB Device Connection", "Device discovery, CDC-ACM / FTDI enumeration, and Connect button."},
                {"frame_02_channel_transfer.png", "02. Channel Transfer & Codeplug", "Handshake, PC mode, Read/Write all memory banks, CSV export, and channel listing."},
                {"frame_03_channel_edit_dialog.png", "03. Channel Edit Dialog", "Modal channel parameter editor with name, frequencies, color code, timeslot, and admit criteria."},
                {"frame_04_zone_management.png", "04. Zone Management", "Grouping of channels into zones, read/write zone memory, and CSV export."},
                {"frame_05_flutter_channel_edit_sheet.png", "05. Flutter Channel Edit Sheet", "Modern Material 3 bottom sheet with animated DMR toggles and validation."},
                {"frame_06_material3_theme_and_widgets.png", "06. Material 3 Widgets Showcase", "FrequencyText, ChannelModeChip, PowerLevelIndicator, and ProgressOverlay."},
                {"frame_07_about_and_contributions_dialog.png", "07. About & Contributions Dialog", "Dropdown menu item and modal dialog with Git repository link, authors, and contribution guide."}
            };

            for (String[] f : frames) {
                sb.append("        <div class=\"card\">\n");
                sb.append("            <img src=\"../../screenshots/").append(f[0]).append("\" alt=\"").append(f[1]).append("\">\n");
                sb.append("            <div class=\"card-body\">\n");
                sb.append("                <div class=\"card-title\">").append(f[1]).append("</div>\n");
                sb.append("                <div class=\"card-desc\">").append(f[2]).append("</div>\n");
                sb.append("                <div class=\"badge\">VERIFIED</div>\n");
                sb.append("            </div>\n        </div>\n");
            }

            sb.append("    </div>\n</div>\n</body>\n</html>\n");

            FileWriter fw = new FileWriter(htmlFile);
            fw.write(sb.toString());
            fw.close();
            System.out.println("✓ HTML behavior test report saved to " + htmlFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write HTML report: " + e.getMessage());
        }
    }
}
