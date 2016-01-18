package net.mephi.swtproject;


import net.mephi.swtproject.components.Board;
import net.mephi.swtproject.components.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import java.lang.reflect.Method;

public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    private final int WIDTH = 600;
    private final int HEIGHT = 600;

    private void start() {
        Display display = new Display();
        Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);

        createTopMenu(shell);


        FillLayout layout = new FillLayout();
        shell.setLayout(layout);


        Board board = new Board(shell);


        InputDialog dlg = new InputDialog(shell);
        String input = dlg.open();
        if (input != null) {
            board.setBallName(input);
        }


        shell.setText("Bubble battle");
        int borW = shell.getSize().x - shell.getClientArea().width;
        int borH = shell.getSize().y - shell.getClientArea().height;
        shell.setSize(WIDTH + borW, HEIGHT + borH);
        shell.open();


        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        //Ресурсы операционной системы
        //должны быть освобождены
        display.dispose();
    }

    //Создание системы меню и всплывающего меню
    public void createTopMenu(Shell shell) {
        // Создание системы меню
        Menu main = createMenu(shell, SWT.BAR | SWT.LEFT_TO_RIGHT);
        shell.setMenuBar(main);

        MenuItem fileMenuItem = createMenuItem(main, SWT.CASCADE, "&Файл",
                null, -1, true, null, shell);
        Menu fileMenu = createMenu(shell, SWT.DROP_DOWN, fileMenuItem, true);
        MenuItem exitMenuItem = createMenuItem(fileMenu, SWT.PUSH, "&Выход\tCtrl+X",
                null, SWT.CTRL + 'X', true, "doExit", shell);

        MenuItem helpMenuItem = createMenuItem(main, SWT.CASCADE, "&Помощь",
                null, -1, true, null, shell);
        Menu helpMenu = createMenu(shell, SWT.DROP_DOWN, helpMenuItem, true);
        MenuItem aboutMenuItem = createMenuItem(helpMenu, SWT.PUSH, "&Об игре\tCtrl+A",
                null, SWT.CTRL + 'A', true, "doAbout", shell);
    }

    // Создание меню с помощью вспомогательных средств
    protected Menu createMenu(Shell parent, int style) {
        Menu m = new Menu(parent, style);
        return m;
    }

    protected Menu createMenu(Shell parent, int style,
                              MenuItem container, boolean enabled) {
        Menu m = createMenu(parent, style);
        m.setEnabled(enabled);
        container.setMenu(m);
        return m;
    }

    protected MenuItem createMenuItem(Menu parent, int style, String text,
                                      Image icon, int accel, boolean enabled,
                                      String callback, Shell shell) {
        MenuItem mi = new MenuItem(parent, style);
        if (text != null) {
            mi.setText(text);
        }
        if (icon != null) {
            mi.setImage(icon);
        }
        if (accel != -1) {
            mi.setAccelerator(accel);
        }
        mi.setEnabled(enabled);
        if (callback != null) {
            registerCallback(mi, this, callback, shell);
        }
        return mi;
    }

    //Вызываемые процедуры Callback для обработки команды меню
    protected void registerCallback(final MenuItem mi,
                                    final Object handler,
                                    final String handlerName, Shell shell) {
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {

                    Class[] cArg = {Shell.class};
                    Method m = handler.getClass().
                            getMethod(handlerName, cArg[0]);
                    m.invoke(handler, shell);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void doAbout(Shell shell) {
        MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("BubbleBattle");
        mb.setMessage("Классная игра, неправда ли?");
        int rc = mb.open();
    }

    public void doExit(Shell shell) {
        System.exit(0);
    }

}
