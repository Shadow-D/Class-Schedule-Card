package bilibili;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Login {
	protected static String baseUrl="http://jxglstu.hfut.edu.cn/eams5-student/";// 網站url
	protected static CloseableHttpClient login=HttpClients.createDefault();// http請求
	protected static ArrayList<String[]> weekDay=new ArrayList<>();// 周几
	protected static ArrayList<String[]> turns=new ArrayList<>();// 第几节
	protected static ArrayList<String[]> room=new ArrayList<>();// 上课地点
	protected static ArrayList<String[]> teacher=new ArrayList<>();// 老师
	protected static ArrayList<String[]> weeksForTeacher=new ArrayList<>();// 教学周，用于匹配老师
	protected static ArrayList<String[]> weeksOfNumber=new ArrayList<>();// 教学周，用于匹配老师
	protected static ArrayList<int[]> weeks=new ArrayList<>();// 教学周
	protected static ArrayList<String> name=new ArrayList<>();// 课程名
	protected static JSONObject result=new JSONObject();// 存放返回的json数据
	protected static String code, semester, startDate, nowWeek;// 存放返回的字符串,學期,開始時間,當前周
	protected static SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");// 日期格式
	protected static CloseableHttpResponse response;// http返回
	protected static Date nowDate=new Date();// 當前時間
	protected static boolean isLogin=false;// 登錄標誌
	protected static int semesterId;// 學期數,粗略計算,以實際爲準,2018年上學期爲33,106行修改
	
	// 登录
	public static boolean login(String user,String password) throws ParseException, IOException {
		String loginUrl=baseUrl+"login";
		JSONObject postData=new JSONObject();// 表单值
		HttpPost loginPost=new HttpPost(loginUrl);
		StringEntity data;
		
		response=login.execute(new HttpGet(baseUrl+"login-salt"));// 一串字符串，用于密码加密
		code=EntityUtils.toString(response.getEntity());
		password=DigestUtils.sha1Hex(code+"-"+password);
		
		loginPost.addHeader("Content-Type", "application/json");// 数据类型
		// 表单传值
		postData.put("username", user);
		postData.put("password", password);
		postData.put("captcha", "");
    	data=new StringEntity(postData.toString());
		loginPost.setEntity(data);
		
		response=login.execute(loginPost);
		code=EntityUtils.toString(response.getEntity());
		result=JSONObject.fromObject(code);
		if (result.get("result").toString().equals("true")) {//　判断返回值
			return true;
		} else {
			return false;
		}
	}

	// 取得课程数据
	public static void getSchedu() throws ClientProtocolException, IOException, java.text.ParseException {
		String nextUrl=baseUrl+"for-std/course-table/";
		String classUrl, studentId;
		HttpGet nextGet=new HttpGet(nextUrl);
		HttpGet classGet;
		JSONArray lessons;
		
		// 通过自动跳转获取学生id
		nextGet.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());// 禁止自动跳转
		response=login.execute(nextGet);
		String newUrl=response.getFirstHeader("Location").toString();
		studentId=(newUrl.substring(newUrl.length()-5));
		
		String zero="2018-2-16";// 學期數
		Date zeroDate=format.parse(zero);
		semesterId=(int)(nowDate.getTime()-zeroDate.getTime())/(1000*60*60*24*30*6)+33;
		
		//　查询数据
		classUrl=baseUrl+"for-std/course-table/get-data?bizTypeId=2&semesterId="+semesterId+"&dataId="+studentId;// 此處修改學期id
		classGet=new HttpGet(classUrl);
		response=login.execute(classGet);
		code=EntityUtils.toString(response.getEntity());
		result=JSONObject.fromObject(code);
		
		// 结果存入json数组，一个值代表一门课程
		lessons=JSONArray.fromObject(result.get("lessons"));
		//　获取学期
		JSONObject title=JSONObject.fromObject(lessons.getJSONObject(0).get("semester"));
		semester=title.getString("nameZh");
		startDate=title.getString("startDate");
		
		for (int i=0;i<lessons.size();i++) {
			//　获取教学周
			JSONArray scheduWeek=JSONArray.fromObject(lessons.getJSONObject(i).get("suggestScheduleWeeks"));
		    int[] week=new int[scheduWeek.size()];
			for (int j=0;j<scheduWeek.size();j++) {
				week[j]=scheduWeek.getInt(j);
			}
			weeks.add(week);
			
			// 获取课程名称
			JSONObject course=JSONObject.fromObject(lessons.getJSONObject(i).get("course"));
			name.add(course.getString("nameZh"));
			
			// 详细信息
			JSONObject schedule=JSONObject.fromObject(lessons.getJSONObject(i).get("scheduleText"));
			JSONObject datePlacePerson=JSONObject.fromObject(schedule.get("dateTimePlacePersonText"));
			String[] text=datePlacePerson.getString("text").split("\n");// 按照不同时间和老师分割
			String[] weekDay1=new String[text.length],week1=new String[text.length],turns1=new String[text.length],teacher1=new String[text.length],room1=new String[text.length];            
			for (int j=0;j<text.length;j++) {
				if (!text[j].equals("null")) {// 课程信息不为空
					String[] information=text[j].split(" ");// 分为不同信息段
					weekDay1[j]=information[0];// 周几
					week1[j]=information[1];// 教学周
					turns1[j]=information[2];// 第几节
					if (information.length==6) {// 有的课可能没有地点
						room1[j]=information[4];// 上课地点
					} else {
						room1[j]=null;
					}
					teacher1[j]=information[information.length-1];// 老师
				} else {// 课程信息为空时
					weekDay1[j]=null;
					turns1[j]=null;
					room1[j]=null;
					teacher1[j]=null;
				}
			}
			// 添加到列表
			turns.add(turns1);
			weeksForTeacher.add(week1);
			teacher.add(teacher1);
			room.add(room1);
			weekDay.add(weekDay1);
		}
	}
	
	public static void dateDeal() throws java.text.ParseException {
		Date start=format.parse(startDate);// 學期開始日期
		long weekNum=(nowDate.getTime()-start.getTime())/(1000*60*60*24*7)+1;// 判斷當前第幾周
		nowWeek=String.valueOf(weekNum);
		
		// 轉換成數字便於定位
		for (String[] s:weekDay) {
			for (int i=0;i<s.length;i++) {
				if (s[i]!=null) {
					switch (s[i]) {
					case "周一":
						s[i]=String.valueOf(0);
						break;
					case "周二":
						s[i]=String.valueOf(1);
						break;
					case "周三":
						s[i]=String.valueOf(2);
						break;
					case "周四":
						s[i]=String.valueOf(3);
						break;
					case "周五":
						s[i]=String.valueOf(4);
						break;
					case "周六":
						s[i]=String.valueOf(5);
						break;
					case "周日":
						s[i]=String.valueOf(6);
						break;
					default:
						break;
					}
				}
			}
		}
		
		for (String[] s:turns) {// 同上
			for (int i=0;i<s.length;i++) {
				if (s[i]!=null) {
					switch (s[i].substring(1, 2)) {
					case "一":
						s[i]=String.valueOf(0);
						break;
					case "三":
						s[i]=String.valueOf(1);
						break;
					case "五":
						s[i]=String.valueOf(2);
						break;
					case "七":
						s[i]=String.valueOf(3);
						break;
					case "九":
						s[i]=String.valueOf(4);
						break;
					default:
						break;
					}
				}
			}
		}
		
		// 取得每節課對應的週數,轉換成數字類型的字符串
		for (String[] s:weeksForTeacher) {// 每門課程
			String[] weekofnum=new String[s.length];
			for (int i=0;i<s.length;i++) {// 每節課程
				String num=null;
				if (s[i]!=null) {
					s[i]=s[i].substring(0, s[i].length()-1);// 去掉"周"字
					String[] str=s[i].split(",");// 考試周的影響,週數會分成兩個部分
					for (int j=0;j<str.length;j++) {
						if (str[j].contains("单")||str[j].contains("双")) {// 單雙週的情況
							str[j]=str[j].substring(0, str[j].length()-3);
							int first=Integer.parseInt(str[j].split("~")[0]);// 首數字
							int last=Integer.parseInt(str[j].split("~")[1]);// 尾數字
							for (int k=first;k<=last;k=k+2) {// 獲取每個周的數字
								if (String.valueOf(k).length()==1) {// 確保週數爲兩位
									if (num==null) {
										num=String.valueOf("0"+String.valueOf(k)+",");
									} else {
										num=num+String.valueOf("0"+String.valueOf(k)+",");
									}
								} else {
									if (num==null) {
										num=String.valueOf(k)+",";
									} else {
										num=num+String.valueOf(k)+",";
									}
								}	
							}
						} else {// 正常情況
							int first=Integer.parseInt(str[j].split("~")[0]);
							int last=Integer.parseInt(str[j].split("~")[1]);
							for (int k=first;k<=last;k++) {
								if (String.valueOf(k).length()==1) {
									if (num==null) {
										num=String.valueOf("0"+String.valueOf(k)+",");
									} else {
										num=num+String.valueOf("0"+String.valueOf(k)+",");
									}
								} else {
									if (num==null) {
										num=String.valueOf(k)+",";
									} else {
										num=num+String.valueOf(k)+",";
									}
								}	
							}
						}
					}
					weekofnum[i]=num;
				} else {
					weekofnum[i]=num;// null,即未規定週數
				}
			}
			weeksOfNumber.add(weekofnum);
		}
	}
	
	// 登錄框
	public static void loginDisplay() {
		@SuppressWarnings("serial")
		class loginDisplay extends JFrame implements ActionListener {
			JLabel userLable, passwordLable;// 提示標籤
			JTextField userText;// 賬號輸入框
			JPasswordField passwordText;//密碼輸入框
			JButton okButton;// 確定按鈕
			public loginDisplay() {
				// TODO Auto-generated constructor stub
				this.setTitle("登录");
				this.setBounds(500, 300, 240, 180);
				this.setLayout(null);
				this.setResizable(false);
				
				userLable=new JLabel("账号:");
				userLable.setFont(new Font("黑体", 1, 20));
				userLable.setBounds(10, 10, 60, 40);
				this.add(userLable);
				
				userText=new JTextField(10);
				userText.setFont(new Font("黑体", 1, 20));
				userText.setBounds(90, 15, 140, 30);
				this.add(userText);
				
				passwordLable=new JLabel("密码:");
				passwordLable.setFont(new Font("黑体", 1, 20));
				passwordLable.setBounds(10, 50, 60, 40);
				this.add(passwordLable);
				
				passwordText=new JPasswordField(15);
				passwordText.setFont(new Font("黑体", 1, 20));
				passwordText.setBounds(90, 55, 140, 30);
				this.add(passwordText);
				
				okButton=new JButton("登录");
				okButton.setFont(new Font("黑体", 1, 20));
				okButton.setBounds(80, 100, 80, 30);
				okButton.addActionListener(this);
				this.add(okButton);
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (e.getSource()==okButton) {
					String user=userText.getText();
					String password=String.valueOf(passwordText.getPassword());
					boolean isNotEmpty=user!=null&&password!=null&&user.length()>0&&password.length()>0;// 判斷是否爲空
					if (isNotEmpty) {
						try {
							if (isLogin=login(user, password)) {// 登錄成功
								try {
									getSchedu();
								} catch (java.text.ParseException e2) {
									// TODO Auto-generated catch block
									e2.printStackTrace();
								}// 獲取數據
								try {
									dateDeal();// 處理數據
								} catch (java.text.ParseException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								this.dispose();// 關閉登錄框
								display();
							} else {// 提示錯誤
								new JOptionPane();
								JOptionPane.showMessageDialog(null, "账号或密码错误", "提示", JOptionPane.ERROR_MESSAGE);
							}
						} catch (ParseException | IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else {// 提示錯誤
						new JOptionPane();
						JOptionPane.showMessageDialog(null, "用户名或密码为空", "提示", JOptionPane.ERROR_MESSAGE);	
					}
				}
			}	
		}
		loginDisplay display=new loginDisplay();// 顯示登錄框
		display.setVisible(true);
	}
	
	// 显示课表
	public static void display() {
		@SuppressWarnings("serial")
		class semesterDisplay extends JFrame implements ActionListener{
			private JPanel toolsPanel, loginPanel, weekPanel, turnsPanel, classPanel;// 頂部面板,退出面板,周面板,節數面板,課程面板
			private JButton loginButton, lastButton, nextButton;// 退出按鈕,前一週按鈕,後一週按鈕
			private JLabel weekLable;// 當前週數
			public semesterDisplay() {
				// TODO Auto-generated constructor stub
				this.setTitle("课程表");
				this.setBounds(300, 60, 1300, 980);
				this.setResizable(false);
				this.setLayout(null);
				
				toolsPanel=new JPanel();
				toolsPanel.setLayout(null);
				toolsPanel.setBounds(0, 0, 1300, 50);
				toolsPanel.setBackground(Color.darkGray);
				lastButton=new JButton("上一周");
				lastButton.setBounds(30, 5, 100, 40);
				lastButton.setBackground(Color.lightGray);
				lastButton.setFont(new Font("黑体", 1, 20));
				lastButton.setForeground(Color.red);
				lastButton.addActionListener(this);
				toolsPanel.add(lastButton);
				nextButton=new JButton("下一周");
				nextButton.setBounds(1170, 5, 100, 40);
				nextButton.setBackground(Color.lightGray);
				nextButton.setFont(new Font("黑体", 1, 20));
				nextButton.setForeground(Color.red);
				nextButton.addActionListener(this);
				toolsPanel.add(nextButton);
				weekLable=new JLabel("test");
				weekLable.setFont(new Font("黑体", 1, 20));
				weekLable.setBounds(600, 5, 100, 40);
				weekLable.setOpaque(true);
				weekLable.setBackground(Color.lightGray);
				weekLable.setForeground(Color.red);
				weekLable.setHorizontalAlignment(SwingConstants.CENTER);
				toolsPanel.add(weekLable);
				this.add(toolsPanel);
				
				loginPanel=new JPanel();
				loginPanel.setLayout(null);
				loginPanel.setBounds(0, 50, 110, 100);
				loginPanel.setBackground(Color.lightGray);
				loginButton=new JButton("退出");
				loginButton.setBounds(15, 30, 80, 40);
				loginButton.setBackground(Color.blue);
				loginButton.setFont(new Font("黑体", 1, 20));
				loginButton.setForeground(Color.red);
				loginButton.addActionListener(this);
				loginPanel.add(loginButton);
				this.add(loginPanel);
				
				
				weekPanel=new JPanel();
				weekPanel.setLayout(null);
				weekPanel.setBounds(110, 50, 1190, 100);
				weekPanel.setBackground(Color.gray);
				String[] weekStr= {"周一","周二","周三","周四","周五","周六","周日"};
				for (int i=0;i<7;i++) {
					JLabel label=new JLabel(weekStr[i]);
					label.setBounds(170*i, 0, 170, 100);
					label.setFont(new Font("黑体", 1, 25));
					label.setForeground(Color.red);
					label.setHorizontalAlignment(SwingConstants.CENTER);
					weekPanel.add(label);
				}
				this.add(weekPanel);
				
				turnsPanel=new JPanel();
				turnsPanel.setLayout(null);
				turnsPanel.setBounds(0, 150, 110, 800);
				turnsPanel.setBackground(Color.gray);
				String[] turnsStr= {"第一节","第二节","第三节","第四节","第五节"};
				for (int i=0;i<5;i++) {
					JLabel label=new JLabel(turnsStr[i]);
					label.setBounds(0, 160*i, 100, 160);
					label.setForeground(Color.darkGray);
					label.setHorizontalAlignment(SwingConstants.CENTER);
					label.setFont(new Font("黑体", 1, 25));
					turnsPanel.add(label);
				}
				this.add(turnsPanel);
				
				classPanel=new JPanel();
				classPanel.setLayout(null);
				classPanel.setBounds(110, 150, 1190, 800);
				classPanel.setBackground(Color.lightGray);
				this.add(classPanel);
			}
			
			public void dateDisplay() {// 顯示信息
				this.setTitle(semester);// 當前學期
				weekLable.setText("第"+nowWeek+"周");// 當前周
				classPanel.removeAll();// 清空當前面板
				Map<Integer[], String[]> locat=new HashMap<>();// 儲存課程信息,用於位置重複時的操作
				for (int i=0;i<weeksOfNumber.size();i++) {// 不同課程種類
					Color color=new Color((int)(256*Math.random()), (int)(256*Math.random()), (int)(256*Math.random()));// 相同課程顏色相同
					String[] weekArray=weeksOfNumber.get(i);
					for (int j=0;j<weekArray.length;j++) {// 不同課程時間
						if (weekArray[j]!=null) {
							if (nowWeek.length()==1) {// 週數爲兩位
								nowWeek="0"+nowWeek;
							}
							if (weekArray[j].contains(nowWeek)) {// 當前周有課
								int x=Integer.parseInt(weekDay.get(i)[j]);// 橫座標,周幾
								int y=Integer.parseInt(turns.get(i)[j]);// 縱座標.第幾節
								String nameShow=name.get(i);// 課程名
								String teacherShow=teacher.get(i)[j];// 老師 
								String roomShow=room.get(i)[j];// 地點
								if (roomShow==null) {// 是否有教室
									roomShow="";
								}
								Integer[] location= {x,y};// 存儲位置,用於比較
								String content=nameShow+"<br/>"+roomShow+"<br/>"+teacherShow;
								String[] show= {content, teacherShow};// 存儲信息,用於修改
								boolean hasKey=false;// 當前位置是否存在課程
								if (locat!=null) {// 排除第一次
									for (Integer[] k:locat.keySet()) {// 循環取得每次課的位置
										if (k[0]==location[0]&&k[1]==location[1]) {//比較
											hasKey=true;
											if (locat.get(k)[0].contains("<br/><br/>")) {//判斷當前位置有幾節課
												if (locat.get(k)[0].split("<br/><br/>")[0].split("<br/>")[0].equals(nameShow)) {//與第一個是否爲同一課程
													if (!locat.get(k)[1].split("-")[0].contains(teacherShow)) {//老師名單是否完整
														String teachers=locat.get(k)[1].split("-")[0]+","+teacherShow+"-"+locat.get(k)[1].split("-")[1];
														String[] newShow= {locat.get(k)[0].split("<br/><br/>")[0]+","+teacherShow+locat.get(k)[0].split("<br/><br/>")[1], teachers};
														locat.put(k, newShow);// 更新信息
													}
													content=locat.get(k)[0];// 顯示數據
												} else if (locat.get(k)[0].split("<br/><br/>")[1].split("<br/>")[0].equals(nameShow)) {//與第二個是否爲同一課程
													if (!locat.get(k)[1].split("-")[1].contains(teacherShow)) {
														String teachers=locat.get(k)[1]+","+teacherShow;
														String[] newShow= {locat.get(k)[0]+","+teacherShow, teachers};
														locat.put(k, newShow);
													}
													content=locat.get(k)[0];
												} else {// 新加第二節課信息
													String teachers=locat.get(k)[1]+"-"+teacherShow;
													String[] newShow= {locat.get(k)[0]+"<br/><br/>"+content, teachers};
													locat.put(k, newShow);
													content=locat.get(k)[0];
												}
											} else {//當前位置只有一節
												if (locat.get(k)[0].split("<br/>")[0].equals(nameShow)) {
													if (!locat.get(k)[1].contains(teacherShow)) {
														String teachers=locat.get(k)[1]+","+teacherShow;
														String[] newShow= {locat.get(k)[0]+","+teacherShow, teachers};
														locat.put(k, newShow);
													}
													content=locat.get(k)[0];
												} else {
													String teachers=locat.get(k)[1]+"-"+teacherShow;
													String[] newShow= {locat.get(k)[0]+"<br/><br/>"+content, teachers};
													locat.put(k, newShow);
													content=locat.get(k)[0];
												}
											}
										    break;
										}
									}
									if (hasKey==false) {// 若該位置沒有課則存儲該課數據
										locat.put(location, show);
									}
								}//顯示
								JLabel lable=new JLabel("<html><body>"+content+"<body/><html/>");
								lable.setFont(new Font("黑体", 1, 18));
								lable.setForeground(Color.black);
								lable.setHorizontalAlignment(SwingConstants.CENTER);
								lable.setBounds(170*x, 160*y, 170, 160);
								lable.setBorder(new LineBorder(Color.black));
					            lable.setOpaque(true);
								lable.setBackground(color);
								classPanel.add(lable, 0);
							}
						}
					}
				}
				classPanel.updateUI();// 更新面板
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (e.getSource()==loginButton) {
					System.exit(0);
				}
				if (e.getSource()==lastButton) {// 上一週
					if (1<Integer.parseInt(nowWeek)&&Integer.parseInt(nowWeek)<21) {
						nowWeek=String.valueOf(Integer.parseInt(nowWeek)-1);// 週數減一
						dateDisplay();
					} 
				}
				if (e.getSource()==nextButton) {// 下一週
					if (0<Integer.parseInt(nowWeek)&&Integer.parseInt(nowWeek)<20) {
						nowWeek=String.valueOf(Integer.parseInt(nowWeek)+1);
						dateDisplay();
					} 
				}
			}
		}
		semesterDisplay di=new semesterDisplay();// 顯示
	    di.dateDisplay();
		di.setVisible(true);
	}
	
	public static void main(String args[]) throws ClientProtocolException, IOException {
		loginDisplay();
	}	
}
