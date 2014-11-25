package eventcsvexporter.v2;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import eventcsvexporter.v2.Engine.EngineListener;


/**
 * ローカルに保存したwalkerplus.comのイベント情報掲載ページ
 * http://www.walkerplus.com/event_list/201412/ar0311/3.html (例)から google
 * calendarにインポート可能なcsvを出力します。
 */
public class MainProg extends JFrame
{

	private static final String APP_TITLE = "EventCsvExporter";

	private static final String TEXT_GO = "ファイルをドロップしてからこのボタンを押して実行";
	private static final String PLEASE_INPUT = "ファイルパスを入力してください。";

	/**
	 * 入力ファイルパスが表示される。
	 */
	private JTextField mTf_input;

	/**
	 * 出力ファイルパスが表示される。
	 */
	private JTextField mTtf_output;

	/**
	 * デフォルトの年が表示される。
	 */
	private JTextField mTf_defYear;

	/**
	 * 処理を実行するときに押すボタン。
	 */
	private JButton mBtn;

	/**
	 * メッセージが表示される。
	 */
	private JLabel mLabel;

	public static void main(String[] args)
	{
		new MainProg();
	}

	/**
	 * コンストラクター。初期設定をする。
	 */
	public MainProg()
	{
		setInterior();
		setExterior();

		// ドラッグ＆ドロップに対応させる
		new DropTarget(this, new MyDropTargetAdapter());
	}

	public class MyDropTargetAdapter extends DropTargetAdapter
	{

		/**
		 * ファイルがドロップされた時に呼び出されます。
		 */
		@Override
		public void drop(DropTargetDropEvent e)
		{
			try
			{
				Transferable t = e.getTransferable();

				if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
				{
					e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

					//ドロップされたファイルのリストを取得
					@SuppressWarnings("unchecked")
					java.util.List<File> fileList = (java.util.List<File>) (t
							.getTransferData(DataFlavor.javaFileListFlavor));

					MainProg.this.callBackOnDrop(fileList);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * 入力ファイルがドロップされた時に呼び出されます。 入力ファイルパスを取得し、出力ファイルパスが自動設定されます。
	 * 
	 * @param fileList
	 */
	public void callBackOnDrop(java.util.List<File> fileList)
	{
		StringBuilder sb = new StringBuilder();
		for (File file : fileList)
		{
			sb.append(file.getAbsolutePath());
			break;
		}

		// 入力ファイルパス
		mTf_input.setText(sb.toString());
		// 出力ファイルパス
		mTtf_output.setText(sb.toString() + ".data.csv");
	}

	/**
	 * テキスト入力やボタンなどを設置します。フォルトの年を設定します。
	 */
	private void setInterior()
	{
		JPanel p1 = new JPanel(new GridLayout(5, 1));

		mTf_input = new JTextField();
		mTtf_output = new JTextField();
		mTtf_output.setEnabled(false);

		mTf_defYear = new JTextField();
		Date d = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(d);

		mTf_defYear.setText(c.get(Calendar.YEAR) + "");

		mBtn = new JButton(TEXT_GO);
		mLabel = new JLabel(PLEASE_INPUT);

		p1.add(mTf_input);
		p1.add(mTtf_output);
		p1.add(mTf_defYear);
		p1.add(mBtn);
		p1.add(mLabel);

		add(p1, BorderLayout.NORTH);

		MyAls als = new MyAls();// アクションリスナー
		mBtn.addActionListener(als);
	}

	/**
	 * ウィンドウのタイトルや大きさなどを設定します。
	 */
	private void setExterior()
	{
		setTitle(APP_TITLE);
		setSize(500, 200);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}

	/**
	 * メッセージを表示します。
	 * 
	 * @param msg
	 */
	private void showMsg(String msg)
	{
		this.mLabel.setText(msg);
	}

	/**
	 * アクションリスナークラスです。クリックを検出します。
	 *
	 */
	public class MyAls implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == mBtn)
			{
				go();// 実行
			}
		}
	}

	/**
	 * 実行部分です。実行ボタンを押して呼び出します。
	 */
	public void go()
	{
		String path_input = mTf_input.getText();
		if (path_input.length() == 0)
		{// 入力ファイルパスがなしならreturn;
			return;
		}
		String path_output = mTtf_output.getText();

		mBtn.setEnabled(false);
		showMsg("実行中");

		MyRn mr = new MyRn(path_input, path_output);
		Thread t = new Thread(mr);
		t.start();// スレッドで実行(進捗を表示するため)
	}

	/**
	 * 処理完了時やエラー発生時にメッセージを表示します。
	 * 
	 * @param ok
	 */
	public void receiv(boolean ok)
	{
		if (ok)
		{
			showMsg("完了。" + PLEASE_INPUT);
			mBtn.setEnabled(true);
		}
		else
		{
			showMsg("エラー発生。アプリを終了してください。");
		}
	}

	/**
	 * スレッドで駆動するランナブルです。進捗表示も担当します。
	 */
	public class MyRn implements Runnable, EngineListener
	{
		/**
		 * 入力ファイルパス
		 */
		private String mpath_in;

		/**
		 * 出力ファイルパス
		 */
		private String mpath_out;

		public MyRn(String mpath_in, String mpath_out)
		{
			this.mpath_in = mpath_in;
			this.mpath_out = mpath_out;
		}

		@Override
		public void run()
		{
			boolean ok = (new Engine(this)).go(mpath_in, mpath_out);
			receiv(ok);
		}

		/**
		 * 進捗を表示します。
		 */
		@Override
		public void reportProgress(long num)
		{
			showMsg(num + "");
		}

		/**
		 * デフォルトの年の文字列を返します。
		 */
		@Override
		public String getDefaultYear()
		{
			return mTf_defYear.getText();
		}
	}

}
