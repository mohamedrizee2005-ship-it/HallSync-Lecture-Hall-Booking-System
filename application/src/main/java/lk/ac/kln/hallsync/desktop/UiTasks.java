package lk.ac.kln.hallsync.desktop;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** JDBC and password hashing run in a worker; Swing widgets stay on the event thread. */
public final class UiTasks {
    private UiTasks() { }
    public static <T> void run(Component owner, String label, Callable<T> task, Consumer<T> success) {
        Window window = owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner);
        JDialog progress = new JDialog(window,"HallSync",Dialog.ModalityType.APPLICATION_MODAL);
        JPanel box = new JPanel(new BorderLayout(10,10)); box.setBorder(BorderFactory.createEmptyBorder(20,24,20,24));
        box.add(new JLabel(label),BorderLayout.NORTH); JProgressBar bar = new JProgressBar(); bar.setIndeterminate(true); box.add(bar);
        progress.setContentPane(box); progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        progress.pack(); progress.setLocationRelativeTo(owner);
        SwingWorker<T,Void> worker = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                progress.dispose();
                try { success.accept(get()); }
                catch (ExecutionException error) { UiErrors.show(owner,error.getCause()); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); UiErrors.show(owner,error); }
                catch (RuntimeException error) { UiErrors.show(owner,error); }
            }
        };
        worker.execute(); progress.setVisible(true);
    }
}
