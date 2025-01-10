package nc.bs.wms.plugin;

import org.apache.commons.lang.ArrayUtils;

import org.json.JSONString;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;

import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.vo.om.hrdept.AggHRDeptVO;
import nc.vo.org.DeptVO;
import nc.vo.org.OrgUnitSaveVO;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.bs.uapbd.tool.HttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.jdbc.framework.processor.ColumnProcessor;

import java.util.Map;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import nc.vo.scmpub.api.rest.utils.RestUtils;

public class OrgUnitAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent event) throws BusinessException {
		// TODO �Զ����ɵķ������
		if (event instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) event;
			Object[] oldObjs = be.getOldObjs();
			Object[] newObjs = be.getNewObjs();
			List<Map> mapList = new ArrayList();
			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof OrgVO) {
					OrgVO ov = (OrgVO)newObjs[i];
					if(ov.getIsbalanceunit()==null)  //��Ϊ���ű༭������ͬʱ������֯���淽����ԭ������Ϊ�˱��ⲿ�ű����ͬ�����ݵ���֯��ɴ��������ø��ֶ���������֯���ǲ���
						continue;
					String code = ov.getCode();  //��֯����
					String name = ov.getName();  //��֯����
					String pk_org = ov.getPk_org();
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String shortname = ov.getShortname()==null?"":ov.getShortname(); //��֯���
					int enableStatus = ov.getEnablestate();
					if(enableStatus==1)
						enableStatus=0;
					else if(enableStatus==2)
						enableStatus=1;
					else
						enableStatus=2;
					Map map = new HashMap();
					map.put("code", code);
					map.put("name", name);
					map.put("orgCode", code);
					map.put("orgName", name);
					map.put("enableStatus", enableStatus);
					map.put("shortName", shortname);
					map.put("srcSystem", "NC");
					map.put("srcSystemId", ov.getPk_org());
					map.put("srcSystemCode", ov.getCode());
					map.put("srcSystemName", ov.getName());
					
					mapList.add(map);
					
				}
			}
			if(mapList==null||mapList.size()==0)
				return;
			JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//			String jsons = js.toString().substring(1, js.toString().length()-1);
			String jsons = js.toString();
			String url = WMSHttpTool.getWMSURL()+"/base/org/ds/save-batch-by-id";
			try {
				String res = WMSHttpTool.sendPost(jsons, url);
				if(res.contains("\"success\":true")) {
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "org_orgs", "NCͬ��WMS��֯");
				}else
					throw new BusinessException(res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "ʧ����Ϣ", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "org_orgs", "NCͬ��WMS��֯");
				throw new BusinessException(e.toString());
			}
		}
		
	}

}
