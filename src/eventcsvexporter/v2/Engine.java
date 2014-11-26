package eventcsvexporter.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Engine
{

	/**
	 * ○○年○○月○○日
	 * ○○/○○/○○
	 * ○○-○○-○○　等にマッチするパターン
	 */
	private static final Pattern sPatTime_1 = Pattern
			.compile("(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+");
	
	/**
	 * ○○月○○日
	 * ○○/○○
	 * ○○-○○　等にマッチするパターン
	 */
	private static final Pattern sPatTime_2 = Pattern
			.compile("(\\d+)\\D+(\\d+)");

	/**
	 * ○○日
	 * ○○　等にマッチするパターン
	 */
	private static final Pattern sPatTime_3 = Pattern
			.compile("(\\d+)");
	
	
	/**
	 * Location（主に市町村区名）にマッチするパターン
	 */
	private static final Pattern sPatPlace = Pattern.compile("●<\\/span>(.+?)<\\/p>",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Subjectにマッチするパターン
	 */
	private static final Pattern sPatName = Pattern.compile("<h\\d+><a .+?>(.+?)<",
			Pattern.CASE_INSENSITIVE);
	
	/**
	 * TimeStrにマッチするパターン
	 */
	private static final Pattern sPatTime = Pattern.compile("【開催日・期間】(.+?)<\\/span>",
			Pattern.CASE_INSENSITIVE);
	
	/**
	 * Description（主に開催場所）にマッチするパターン
	 */
	private static final Pattern sPatDescription = Pattern.compile("【開催場所】(.+?)<\\/span>",
			Pattern.CASE_INSENSITIVE);
	
	
	/**
	 * 日付を出力する際に使う区切り文字列
	 */
	private static final String TIME_DELIMIT = "-";

	/**
	 * Location格納
	 */
	private ArrayList<String> mAry_Location;

	/**
	 * Subject格納
	 */
	private ArrayList<String> mAry_Subject;
	
	/**
	 * TimeStr(Start,Endに分解前の文字列)格納
	 */
	private ArrayList<String> mAry_TimeStr;
	
	/**
	 * Description格納
	 */
	private ArrayList<String> mAry_Description;

	/**
	 * クラス間通信インターフェース
	 */
	public interface EngineListener
	{
		/**
		 * 進捗をリポートする
		 * @param num
		 */
		public void reportProgress(long num);
		
		/**
		 * デフォルトの年を返す
		 * @return
		 */
		public String getDefaultYear();
	}
	
	/**
	 * クラス間通信インターフェースのリスナー
	 */
	private EngineListener mListener;
	
	public Engine(EngineListener lis)
	{
		mListener = lis;
	}

	public boolean go(String path, String outpath)
	{
		mAry_Location = new ArrayList<String>();
		mAry_Subject = new ArrayList<String>();
		mAry_TimeStr = new ArrayList<String>();
		mAry_Description = new ArrayList<String>();

		try
		{
			// 入力
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(path)));
			// debug
			System.out.println(">from " + path + "\n>to " + outpath);

			// output to mAry_s
			boolean sizeCheck = readToArys(br);
			// after output, close.
			br.close();

			if (sizeCheck == false)
			{// 要素数が合わないエラー発生。
				return false;
			}
			else
			{
				System.out.println("sizeCheck ok.");
			}

			// 出力の用意
			PrintWriter pw = new PrintWriter(//
					new BufferedWriter(new FileWriter(outpath)));
			//出力する
			writeFromArysToOutput(pw);
			pw.close();

			System.out.println("END.");
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * CSVファイルに出力する
	 * @param pw
	 */
	private void writeFromArysToOutput(PrintWriter pw)
	{
		// header write
		pw.println("\"Subject\",\"Start Date\",\"End Date\",\"Description\",\"Location\"");

		for (int i = 0; i < mAry_Subject.size(); i++)
		{
			String time = mAry_TimeStr.get(i).split("※")[0];
			String[] tAry = time.split("～");
			
			String timeS = formatedTime(tAry[0], "");
			String timeE = (tAry.length > 1) ? formatedTime(tAry[1], timeS) : timeS;

			pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n", //
					mAry_Subject.get(i), //
					timeS, //
					timeE, //
					mAry_Description.get(i), //
					mAry_Location.get(i));
			
			mListener.reportProgress(i + 1);
		}
	}
	
	/**
	 * 形式を整えた日時文字列を返す。
	 * @param t　整える前の日時文字列
	 * @param smpl　整える際に参考にする日時文字列
	 * @return
	 */
	private String formatedTime(String t, String smpl)
	{
		Matcher m;
		
		if ((m = sPatTime_1.matcher(t)).find())
		{
			return m.group(1) + TIME_DELIMIT + m.group(2) + TIME_DELIMIT + m.group(3);
		}
		else if ((m = sPatTime_2.matcher(t)).find())
		{
			return mListener.getDefaultYear() + TIME_DELIMIT + //
					m.group(1) + TIME_DELIMIT + m.group(2);
		}
		else if((m = sPatTime_3.matcher(t)).find())
		{
			//参考文字列を分解する
			String ary[] = smpl.split(TIME_DELIMIT);
			//参考文字列の年と月を転用する
			return ary[0] + TIME_DELIMIT + ary[1] + TIME_DELIMIT + m.group(1);
		}

		return t;
	}

	/**
	 * 格納用ArrayListにLocation,Subject,Description,TimeStrを格納する
	 * @param br
	 * @return
	 */
	private boolean readToArys(BufferedReader br)
	{
		long lineCnt = 0;
		try
		{
			String line = null;
			while ((line = br.readLine()) != null)
			{
				Matcher m;

				if ((m = sPatPlace.matcher(line)).find())
				{
					mAry_Location.add(m.group(1));
					System.out.println("Location:" + m.group(1));
				}
				else if ((m = sPatName.matcher(line)).find())
				{
					mAry_Subject.add(m.group(1));
					System.out.println("Subject:" + m.group(1));
				}
				else if ((m = sPatTime.matcher(line)).find())
				{
					mAry_TimeStr.add(m.group(1));
					System.out.println("TimeStr:" + m.group(1));
				}
				else if ((m = sPatDescription.matcher(line)).find())
				{
					mAry_Description.add(m.group(1));
					System.out.println("Description:" + m.group(1) + "\n");
				}
				else
				{
					// nothing
				}

				mListener.reportProgress(++lineCnt);
			}
		}
		catch (IOException e)
		{
			System.out.println("readLine error:" + e);
			return false;
		}

		//各要素数が同じかチェックする
		if (mAry_Description.size() == mAry_Location.size() && //
				mAry_Description.size() == mAry_Subject.size() && //
				mAry_Description.size() == mAry_TimeStr.size())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
