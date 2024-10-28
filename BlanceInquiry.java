import org.brotli.dec.BrotliInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class BlanceInquiry {
    private static final String CONFIG_FILE = "config.txt";
    public static void main(String[] args) {
        if(isConfigIncomplete()){
            CredentialsDialog();//加载或创建配置文件
        }else {
            displayBalanceMessages();
            if(BiliVideoProfit() == null||BiliShopmallBalance()==null){
                JOptionPane.showMessageDialog(null, "登录Cookie过期","Cookie过期", JOptionPane.ERROR_MESSAGE);
                CredentialsDialog();
            }
            else if(BiliVideoUnsettledProfit()==null){
                JOptionPane.showMessageDialog(null, "激励Cookie过期","Cookie过期", JOptionPane.ERROR_MESSAGE);
                CredentialsDialog();
            }
            else if(BiliShopmallUnsettledProfit()==null){
                JOptionPane.showMessageDialog(null, "工房Cookie过期","Cookie过期", JOptionPane.ERROR_MESSAGE);
                CredentialsDialog();
            }
            else if(BalanceEnquiry172()==null){
                JOptionPane.showMessageDialog(null, "172号Token过期","Token过期", JOptionPane.ERROR_MESSAGE);
                CredentialsDialog();
            }
        }



    }
    private static void displayBalanceMessages() {
        /*B站*/
        // 查询视频结算贝壳
        BigDecimal withdrawableBrokerage= BiliVideoProfit();
        // 查询视频未结算贝壳
        BigDecimal unwithdraw_income=BiliVideoUnsettledProfit();
        // 查询工坊可提现收益
        BigDecimal withDrawBalanceAmount = BiliShopmallBalance();
        // 查询工坊未结算收益
        BigDecimal totalSettleMoney=BiliShopmallUnsettledProfit();

        /*查询172可提现收益*/
        BigDecimal blance = BalanceEnquiry172();


        // 使用SwingUtilities.invokeLater来确保GUI更新在事件调度线程中执行
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("收益信息");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ImageIcon icon = new ImageIcon("resource//IMG\\收益.png");
            frame.setIconImage(icon.getImage());//给窗体设置图标方法
            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null); // 窗口居中

            // 创建一个面板来容纳所有的信息
            JPanel panel = new JPanel();
            frame.setResizable(false); //设置窗口不可调整大小
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加边距

            //设置字体
            Font labelFont = new Font("微软雅黑", Font.BOLD, 14);

            // B站工坊
            BigDecimal zero = new BigDecimal("0.00");
            BigDecimal hundred = new BigDecimal("100.00");
            double withDrawBalanceAmount_zero = withDrawBalanceAmount.compareTo(zero);
            double totalSettleMoney_zero = totalSettleMoney.compareTo(zero);
            BigDecimal sum = withDrawBalanceAmount.add(totalSettleMoney);
            String message1 = ((withDrawBalanceAmount_zero>=0 && totalSettleMoney_zero>0)?"B站工坊 总收益= " +"(已结算)" +  withDrawBalanceAmount +"+(待结算)"+totalSettleMoney+"="+sum+ " 元":"B站贝壳 总收益= "+sum + " 元");

            JLabel label1 = new JLabel(message1, SwingConstants.CENTER);
            label1.setFont(labelFont);
            panel.add(label1);
            panel.add(Box.createRigidArea(new Dimension(10, 10))); // 添加间距



            // B站贝壳

            double withdrawableBrokerage_zero = withdrawableBrokerage.compareTo(zero);
            double unwithdraw_income_zero = unwithdraw_income.compareTo(zero);
            double withdrawableBrokerage_hundred= withdrawableBrokerage.compareTo(hundred);
            BigDecimal sum2 = withdrawableBrokerage.add(unwithdraw_income);
            String message2 = ((withdrawableBrokerage_zero>=0&&unwithdraw_income_zero>0)?"B站贝壳 总收益= "+"(已结算)" + withdrawableBrokerage+"+(待结算)"+unwithdraw_income+"="+sum2+ " 元":"B站贝壳 总收益= "+sum2 + " 元");
            JLabel label2 = new JLabel(message2, SwingConstants.CENTER);
            label2.setFont(labelFont);
            panel.add(label2);
            panel.add(Box.createRigidArea(new Dimension(10, 10))); // 添加间距


            // 172
            double blance_zero=blance.compareTo(zero);
            String message3 = ("172号卡 总收益= " +blance+" 元");
            JLabel label3 = new JLabel(message3, SwingConstants.CENTER);
            label3.setFont(labelFont);
            panel.add(label3);
            panel.add(Box.createRigidArea(new Dimension(10, 20))); // 添加间距


            //总计
            BigDecimal Sum3 = withdrawableBrokerage.add(blance).add(withDrawBalanceAmount);
            BigDecimal Sum2 = blance.add(withDrawBalanceAmount);
            String message=  (((withDrawBalanceAmount_zero > 0 || blance_zero > 0)&&withdrawableBrokerage_hundred>=100)? "已产生 "+Sum3+" 元的总收益，赶快去提现吧！":"可提现总收益:  "+Sum2+" 元,请继续努力！");
            JLabel label = new JLabel(message, SwingConstants.CENTER);
            label.setFont(labelFont);
            panel.add(label);

            // 将面板添加到frame中
            frame.add(panel);

            // 显示frame
            frame.setVisible(true);
        });
    }
    //贝壳查询
    private static BigDecimal BiliVideoProfit() {
        String requestUrl = "https://pay.bilibili.com/bk/brokerage/getUserBrokerage";
        String jsonInputString = "{\"traceId\":\"1729407720000\",\"timestamp\":\"1729407720000\",\"sdkVersion\":\"1.2.1\"}";
        String cookie = getConfig("登录Cookie");
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(requestUrl);
            connection = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法和请求头
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Host", "pay.bilibili.com");
            connection.setRequestProperty("Content-Length", Integer.toString(jsonInputString.getBytes().length));
            connection.setRequestProperty("sec-ch-ua", "\"Not=A?Brand\";v=\"99\", \"Chromium\";v=\"118\"");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("sec-ch-ua-platform", "Windows");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            connection.setRequestProperty("sec-ch-ua-platform", "Windows");
            connection.setRequestProperty("Origin", "https://pay.bilibili.com");
            connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
            connection.setRequestProperty("Sec-Fetch-Mode", "cors");
            connection.setRequestProperty("Sec-Fetch-Dest", "empty");
            connection.setRequestProperty("Referer", "https://pay.bilibili.com/pay-v2-web/shell_index");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            connection.setRequestProperty("Cookie", cookie);// 设置Cookie，确保替换成有效的Cookie字符串

            // 发送POST请求
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes("UTF-8"));
            }
            int responseCode = connection.getResponseCode();

            // 读取响应
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                //System.out.println("POST Response Code : " + responseCode+","+response.toString());
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject dataObject = jsonObject.getJSONObject("data");

                double Brokerage = dataObject.getDouble("withdrawableBrokerage");
                BigDecimal withdrawableBrokerage = new BigDecimal(Brokerage).setScale(2, BigDecimal.ROUND_HALF_UP);

                return withdrawableBrokerage;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //未结算的贝壳
    private static BigDecimal BiliVideoUnsettledProfit() {
        String urlString = "https://api.bilibili.com/studio/growup/web/up/wallet/summary?s_locale=zh_CN";
        String cookie1 = getConfig("激励Cookie");
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 设置请求方法
            conn.setRequestMethod("GET");
            // 添加请求头
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            conn.setRequestProperty("Accept-Encoding", "br");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            conn.setRequestProperty("Cookie", cookie1);

// 读取响应
            int responseCode = conn.getResponseCode();


            // 读取响应
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BrotliInputStream(conn.getInputStream()), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // 打印结果
                //System.out.println("POST Response Code : " + responseCode+","+response.toString());
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject dataObject = jsonObject.getJSONObject("data");

                double income = dataObject.getDouble("unwithdraw_income")/100;
                BigDecimal unwithdraw_income = new BigDecimal(income).setScale(2, BigDecimal.ROUND_HALF_UP);


                return unwithdraw_income;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //工坊已结算收益
    private static BigDecimal BiliShopmallBalance() {
        String urlString = "https://pay.bilibili.com/merchant/memberAccount/v2/queryMemberAccount";
        String jsonInputString = "{\"customerId\":\"10051\",\"accountId\":\"13618808664523120640\"}";
        String cookie = getConfig("登录Cookie");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Host", "pay.bilibili.com");
            conn.setRequestProperty("Content-Length", String.valueOf(jsonInputString.getBytes(StandardCharsets.UTF_8).length));
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Origin", "https://pay.bilibili.com");
            conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
            conn.setRequestProperty("Sec-Fetch-Mode", "cors");
            conn.setRequestProperty("Sec-Fetch-Dest", "empty");
            conn.setRequestProperty("Referer", "https://pay.bilibili.com/pay-v2/star/index?accountId=13618808664523120640&noTitleBar=1&customerId=10051&from=up-data_earnings&for_model=mine_buyer&msource=mine&night=1&native.theme=2");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            conn.setRequestProperty("Cookie", cookie);

            // 发送POST请求
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();


            // 读取响应
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream()), "UTF-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject dataObject = jsonObject.getJSONObject("data");
                double withDrawBalance = dataObject.getDouble("withDrawBalanceAmount");
                BigDecimal withDrawBalanceAmount = new BigDecimal(withDrawBalance).setScale(2, BigDecimal.ROUND_HALF_UP);
                return withDrawBalanceAmount;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //工坊未结算收益
    private static BigDecimal BiliShopmallUnsettledProfit() {
        String urlString = "https://mall.bilibili.com/mall-up-c/shop/settle/query";
        String jsonInputString = "{\"pageNum\":1,\"pageSize\":10,\"prePageIds\":[]}";
        String cookie2 = getConfig("工房Cookie");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            // 设置允许输入
            conn.setDoOutput(true);
            // 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept-Encoding", "br");
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            conn.setRequestProperty("Cookie", cookie2);

            // 发送POST请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new BrotliInputStream(conn.getInputStream()), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // 解析响应
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    JSONArray shopSettleDetailList = dataObject.getJSONArray("shopSettleDetailList");

                    double total = 0;
                    for (int i = 0; i < shopSettleDetailList.length(); i++) {
                        JSONObject item = shopSettleDetailList.getJSONObject(i);
                        double settleMoney = item.getDouble("settleMoney")/100;
                        total+= settleMoney;

                    }
                    BigDecimal totalSettleMoney = new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP);
                    return totalSettleMoney;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //172查询
    private static BigDecimal BalanceEnquiry172(){
        String urlString = "https://haokaapi.lot-ml.com/api/Income/Tongji";
        String token = getConfig("172号Token");

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Host", "haokaapi.lot-ml.com");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("sec-ch-ua", "\"Not=A?Brand\";v=\"99\", \"Chromium\";v=\"118\"");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("sec-ch-ua-mobile", "?0");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            conn.setRequestProperty("sec-ch-ua-platform", "Windows");
            conn.setRequestProperty("Origin", "https://haoka.lot-ml.com");
            conn.setRequestProperty("Sec-Fetch-Site", "same-site");
            conn.setRequestProperty("Sec-Fetch-Mode", "cors");
            conn.setRequestProperty("Sec-Fetch-Dest", "empty");
            conn.setRequestProperty("Referer", "https://haoka.lot-ml.com/");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");

            int responseCode = conn.getResponseCode();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))){
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                //System.out.println("POST Response Code : " + responseCode+","+response.toString());

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject dataObject = jsonObject.getJSONObject("data");

                double Balance = dataObject.getDouble("blance");
                BigDecimal blance = new BigDecimal(Balance).setScale(2, BigDecimal.ROUND_HALF_UP);

                return blance;
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();

        }
        return null;
    }


        public static void CredentialsDialog() {
            JFrame frame = new JFrame("修改cookie");//设置窗口的title
            //frame.setResizable(false); //设置窗口不可调整大小
            frame.setSize(360, 320); // 设置窗口的初始大小
            ImageIcon icon = new ImageIcon("resource//IMG\\Cookie.png");
            frame.setIconImage(icon.getImage());//给窗体设置图标方法

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置窗口关闭后关闭程序
            frame.setLocationRelativeTo(null);//居中定位

            JPanel panel = new JPanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


            JLabel labelCookie = new JLabel("登录Cookie:");
            JTextField textFieldCookie = new JTextField(10);
            textFieldCookie.setPreferredSize(new Dimension(70, 25));
            textFieldCookie.setFont(new Font("微软雅黑", Font.BOLD, 20));

            JLabel labelCookie1 = new JLabel("激励Cookie:");
            JTextField textFieldCookie1 = new JTextField(10);
            textFieldCookie1.setPreferredSize(new Dimension(70, 25));
            textFieldCookie1.setFont(new Font("微软雅黑", Font.BOLD, 20));

            JLabel labelCookie2 = new JLabel("工房Cookie:");
            JTextField textFieldCookie2 = new JTextField(10);
            textFieldCookie2.setPreferredSize(new Dimension(70, 25));
            textFieldCookie2.setFont(new Font("微软雅黑", Font.BOLD, 20));

            JLabel labelToken = new JLabel("172' Token:");
            JTextField textFieldToken = new JTextField(10);
            textFieldToken.setPreferredSize(new Dimension(70, 25));
            textFieldToken.setFont(new Font("微软雅黑", Font.BOLD, 20));

            JButton buttonSave = new JButton("   保  存   ");
            buttonSave.setBackground(new Color(0, 122, 255)); // 蓝色
            buttonSave.setForeground(Color.WHITE); // 白色文字
            buttonSave.setFont(new Font("微软雅黑", Font.BOLD, 18));
            buttonSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String Cookie=textFieldCookie.getText().trim();
                    String Cookie1=textFieldCookie1.getText().trim();
                    String Cookie2=textFieldCookie2.getText().trim();
                    String Token=textFieldToken.getText().trim();

                    saveConfig(Cookie, Cookie1,Cookie2,Token);
                    JOptionPane.showMessageDialog(null, "保存配置成功！");
                    frame.dispose();

                }
            });

                    panel.add(labelCookie);

                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(textFieldCookie);

                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(labelCookie1);

                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(textFieldCookie1);
                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(labelCookie2);
                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(textFieldCookie2);
                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(labelToken);
                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(textFieldToken);
                    panel.add(Box.createHorizontalStrut(0));
                    panel.add(Box.createVerticalStrut(50));

                    panel.add(buttonSave);



                    frame.add(panel);
                    frame.setVisible(true);
                }


    private static boolean isConfigIncomplete() {
        try {
            String content = Files.readString(Paths.get(CONFIG_FILE), StandardCharsets.UTF_8);
            return !content.contains("登录Cookie=") || !content.contains("激励Cookie=") || !content.contains("工房Cookie=")|| !content.contains("172号Token");
        } catch (IOException e) {
            return true;
        }
    }

    //保存创建的config.txt配置文件，获取填写在面板的信息并写入文件
    private static void saveConfig(String Cookie, String Cookie1,String Cookie2,String Token) {
        String content = String.format("登录Cookie=%s\n激励Cookie=%s\n工房Cookie=%s\n172号Token=%s\n", Cookie, Cookie1, Cookie2,Token);
        try {
            Files.writeString(Paths.get(CONFIG_FILE), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + File.separator + "config.txt";
    private static String getConfig(String key) {
        try {
            String content = Files.readString(Paths.get(CONFIG_FILE_PATH), StandardCharsets.UTF_8);
            for (String line : content.lines().toList()) {
                if (line.startsWith(key + "=")) {
                    return line.substring(key.length() + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}


