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
     * ����post����
     * 
     * @author Michael -----CSDN: http://blog.csdn.net/capmiachael
     * @param params
     *            ����
     * @param requestUrl
     *            �����ַ
     * @param authorization
     *            ��Ȩ��
     * @return ���ؽ��
     * @throws IOException
     */
    public static String sendPost(String params, String requestUrl) throws IOException {

        byte[] requestBytes = params.getBytes("utf-8"); // ������תΪ��������
        HttpClient httpClient = new HttpClient();// �ͻ���ʵ����
        PostMethod postMethod = new PostMethod(requestUrl);
        //��������ͷAuthorization
//        postMethod.setRequestHeader("Authorization", "Basic " + authorization);
        // ��������ͷ  Content-Type
        String sessionID = getsessionID();
        postMethod.setRequestHeader("Content-Type", "application/json");
//        postMethod.setRequestHeader("session_id", sessionID);
        postMethod.setRequestHeader("Authorization", sessionID);
        InputStream inputStream = new ByteArrayInputStream(requestBytes, 0,
                requestBytes.length);
        RequestEntity requestEntity = new InputStreamRequestEntity(inputStream,
                requestBytes.length, "application/json; charset=utf-8"); // ������
        postMethod.setRequestEntity(requestEntity);
        httpClient.executeMethod(postMethod);// ִ������
        InputStream soapResponseStream = postMethod.getResponseBodyAsStream();// ��ȡ���ص���
        byte[] datas = null;
        try {
            datas = readInputStream(soapResponseStream);// ���������ж�ȡ����
        } catch (Exception e) {
            e.printStackTrace();
        }
        String result = new String(datas, "UTF-8");// ����������תΪString
        // ��ӡ���ؽ��
        // System.out.println(result);

        return result;

    }
    
    /**
                * ��ȡsession_id
     * @param params
     * @param requestUrl
     * @return
     * @throws IOException
     */
    public static String sendPost2(String requestUrl) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost request = new HttpPost(requestUrl);
        
        // ��������ͷ����Ϣ
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Authorization", "Basic dGVzdDp0ZXN0");
        
        // ����POST����
        StringBuilder paramsBuilder = new StringBuilder()
                .append("username=").append("test1") // �û��� 
                .append("&").append("password=").append("BC4yg0LGvuxY5ibY%2BY0VHF0%3D") // ����  �����룺aiw5gkaxoOo=
                .append("&").append("grant_type=").append("password"); // grant_typeΪpassword
                
        // ������ת�����ַ���ʵ�岢��ӵ�������
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
                    // TODO: �����ؽ��
        } else {
        	client.close();
        	throw new BusinessException("����ʧ�ܣ�״̬�룺" + statusCode);
        }
    }

    /**
     * ���������ж�ȡ����
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
	 * ��ȡMESϵͳ��ip���ַ
	 * ���ӣ�http://127.0.0.1:8083
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
			// TODO �Զ����ɵ� catch ��
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