package nc.bs.wms.tool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONString;

import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.httpclient.NameValuePair;

import nc.bs.framework.common.NCLocator;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.pub.BusinessException;
import nc.vo.scmpub.api.rest.utils.RestUtils;

public class WMSHttpTool {

    /**
     * 发送post请求
     * 
     * @author Michael -----CSDN: http://blog.csdn.net/capmiachael
     * @param params
     *            参数
     * @param requestUrl
     *            请求地址
     * @param authorization
     *            授权书
     * @return 返回结果
     * @throws IOException
     */
    public static String sendPost(String params, String requestUrl) throws IOException {

        byte[] requestBytes = params.getBytes("utf-8"); // 将参数转为二进制流
        HttpClient httpClient = new HttpClient();// 客户端实例化
        PostMethod postMethod = new PostMethod(requestUrl);
        //设置请求头Authorization
//        postMethod.setRequestHeader("Authorization", "Basic " + authorization);
        // 设置请求头  Content-Type
        String sessionID = getsessionID();
        postMethod.setRequestHeader("Content-Type", "application/json");
//        postMethod.setRequestHeader("session_id", sessionID);
        postMethod.setRequestHeader("Authorization", sessionID);
        InputStream inputStream = new ByteArrayInputStream(requestBytes, 0,
                requestBytes.length);
        RequestEntity requestEntity = new InputStreamRequestEntity(inputStream,
                requestBytes.length, "application/json; charset=utf-8"); // 请求体
        postMethod.setRequestEntity(requestEntity);
        httpClient.executeMethod(postMethod);// 执行请求
        InputStream soapResponseStream = postMethod.getResponseBodyAsStream();// 获取返回的流
        byte[] datas = null;
        try {
            datas = readInputStream(soapResponseStream);// 从输入流中读取数据
        } catch (Exception e) {
            e.printStackTrace();
        }
        String result = new String(datas, "UTF-8");// 将二进制流转为String
        // 打印返回结果
        // System.out.println(result);

        return result;

    }
    
    /**
                * 获取session_id
     * @param params
     * @param requestUrl
     * @return
     * @throws IOException
     */
    public static String sendPost2(String requestUrl) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost request = new HttpPost(requestUrl);
        
        // 设置请求头部信息
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Authorization", "Basic dGVzdDp0ZXN0");
        
        // 构建POST参数
        StringBuilder paramsBuilder = new StringBuilder()
                .append("username=").append("test1") // 用户名 
                .append("&").append("password=").append("BC4yg0LGvuxY5ibY%2BY0VHF0%3D") // 密码  旧密码：aiw5gkaxoOo=
                .append("&").append("grant_type=").append("password"); // grant_type为password
                
        // 将参数转换成字符串实体并添加到请求中
        StringEntity entity = new StringEntity(paramsBuilder.toString());
        request.setEntity(entity);
        CloseableHttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity2 = response.getEntity();
            String responseBody = EntityUtils.toString(entity2);
            JSONObject js = (JSONObject)JSONObject.parse(responseBody);
            String token_type = js.getString("token_type");
            String access_token = js.getString("access_token");
            client.close();
            return token_type+" "+access_token;
                    // TODO: 处理返回结果
        } else {
        	client.close();
        	throw new BusinessException("请求失败，状态码：" + statusCode);
        }
    }

    /**
     * 从输入流中读取数据
     * 
     * @param inStream
     * @return
     * @throws Exception
     */
    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }
    
    /**
	 * 获取MES系统的ip与地址
	 * 例子：http://127.0.0.1:8083
	 * @return
	 */
	public static String getWMSURL(){
		String sql = "select value from pub_sysinit where initcode = 'WMSURL'";
		String url = null;
		try {
			url = (String) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql, new ColumnProcessor());
			if(url != null){
				return url;
			}
		} catch (BusinessException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return url;
	}
	
	public static String getsessionID() {
//		String json = "{\"params\":{\"login\":\"admin\"}}";
		String url = getWMSURL()+"/auth/oauth/token";
		try {
//			return sendPost2(json,url);
			return sendPost2(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
}