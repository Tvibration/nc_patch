package nc.itf.rest.wms;


import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONString;

//import uap.json.JSONObject;
import com.alibaba.fastjson.JSONObject;

import nc.vo.scmpub.res.Module;


@Path("/WmsService")
public class IRestForWMS extends uap.ws.rest.resource.AbstractUAPRestResource{

	@Override
	public String getModule() {
		// TODO �Զ����ɵķ������
		return Module.SO.getName();
	}

	@POST
	@Path("/Generate45")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate45(JSONObject jsons){	  //���ɲɹ����
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate45(jsons);

//		return RestUtils.toJSONString(result);
	}
	
	@POST
	@Path("/Generate4D")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4D(JSONObject jsons){	  //���ɲ��ϳ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4D(jsons);
	}
	
	@POST
	@Path("/Generate4D_2")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4D_2(JSONObject jsons){	  //�������������β��ϳ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4D_2(jsons);
	}
	
	@POST
	@Path("/Rewrite4D")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Rewrite4D(JSONObject jsons){	  //��д���ϳ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Rewrite4D(jsons);
	}
	
	@POST
	@Path("/Generate4I")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4I(JSONObject jsons){	  //������������
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4I(jsons);
	}
	
	@POST
	@Path("/Rewrite4I")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Rewrite4I(JSONObject jsons){	  //��д��������
		
		RestForWMSimpl2 rm = new RestForWMSimpl2();
		return rm.Rewrite4I(jsons);
	}
	
	@POST
	@Path("/Generate4A")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4A(JSONObject jsons){	  //�����������
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4A(jsons);
	}
	
	@POST
	@Path("/Generate46")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate46(JSONObject jsons){	  //���ɲ���Ʒ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate46(jsons);
	}
	
	@POST
	@Path("/Generate46v2")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate46v2(JSONObject jsons){	  //���ɲ���Ʒ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate46v2(jsons);
	}
	
	@POST
	@Path("/Generate46_NS")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate46_NS(JSONObject jsons){	  //���������β���Ʒ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate46_NS(jsons);
	}
	
	@POST
	@Path("/Generate46_oth")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate46_oth(JSONObject jsons){	  //����������Ʒ���
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate46_oth(jsons);
	}
	
	@POST
	@Path("/Generate4Y")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4Y(JSONObject jsons){	  //���ɵ�������
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4Y(jsons);
	}
	
	@POST
	@Path("/Generate4E")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4E(JSONObject jsons){	  //���ɵ������
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4E(jsons);
	}
	
	@POST
	@Path("/Generate4K")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4K(JSONObject jsons){	  //����ת�ⵥ
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4K(jsons);
	}
	
	@POST
	@Path("/Rewrite4K")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Rewrite4K(JSONObject jsons){	  //��дת�ⵥ
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Rewrite4K(jsons);
	}
	
	@POST
	@Path("/Generate23")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate23(JSONObject jsons){	  //���ɵ�����
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate23(jsons);
	}
	
	@POST
	@Path("/Generate2345")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate2345(JSONObject jsons){	  //���յ��������ɲɹ����
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate2345(jsons);
	}
	
	@POST
	@Path("/Update45")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Update45(JSONObject jsons){	  //���ֲɹ����(�˿�)
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Update45(jsons);
	}
	
	@POST
	@Path("/Generate4H")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4H(JSONObject jsons){	  //���ɽ����
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4H(jsons);
	}
	
	@POST
	@Path("/Generate49")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate49(JSONObject jsons){	  //���ɽ��뵥
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate49(jsons);
	}
	
	@POST
	@Path("/Generate4B")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4B(JSONObject jsons){	  //���ɽ������
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4B(jsons);
	}
	
	@POST
	@Path("/Generate4J")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Generate4J(JSONObject jsons){	  //���ɽ��뻹��
		
		RestForWMSimpl rm = new RestForWMSimpl();
		return rm.Generate4J(jsons);
	}
	
	@POST
	@Path("/SendWMS21")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString SendWMS21(JSONObject jsons){	  //�ֶ���WMS�ɹ�����
		
		RestForWMSimpl2 rm = new RestForWMSimpl2();
		return rm.SendWMS21(jsons);
	}
	
	@POST
	@Path("/Delete4Y")
	@Produces("application/json")
	@Consumes("application/json")
	public JSONString Delete4Y(JSONObject jsons){	  //ɾ����������
		
		RestForWMSimpl2 rm = new RestForWMSimpl2();
		return rm.Delete4Y(jsons);
	}
}
