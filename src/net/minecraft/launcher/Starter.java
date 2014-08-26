package net.minecraft.launcher;

import java.awt.Font;
import java.io.File;
import java.net.Proxy;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class Starter extends JFrame
{
	private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
	  private final JTextArea textArea;
	  private final JScrollPane scrollPane;
	 /**
	 *
	 */
	public Starter()
	{
		super("Minecraft Launcher");

	    setSize(854, 480);
	    setDefaultCloseOperation(3);

	    this.textArea = new JTextArea();
	    this.textArea.setLineWrap(true);
	    this.textArea.setEditable(false);
	    this.textArea.setFont(MONOSPACED);
	    ((DefaultCaret)this.textArea.getCaret()).setUpdatePolicy(1);

	    this.scrollPane = new JScrollPane(this.textArea);
	    this.scrollPane.setBorder(null);
	    this.scrollPane.setVerticalScrollBarPolicy(22);

	    add(this.scrollPane);
	    setLocationRelativeTo(null);
	    setVisible(true);
	    File workdir = new File("c:\\temp\\mojang.launcher");

	    if(!workdir.exists())
	    	workdir.mkdir();

		 new Launcher(this, workdir, Proxy.NO_PROXY, null, new String[]{"-Xmx1G"}, 6);

	}
	public static void main(String[] args)
	  {
		new Starter();
	  }
}
