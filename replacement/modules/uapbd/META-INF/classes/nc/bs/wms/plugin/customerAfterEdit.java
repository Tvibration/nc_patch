package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.wms.tool.WMSHttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.cust.CustomerVO;
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.material.marbasclass.MarBasClassVO;
import nc.vo.org.DeptVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnProcessor;

public class customerAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO Auto-generated method stub
		if (arg0 instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) arg0;
			Object[] newObjs = be.getNewObjs();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			List<Map> mapList = new ArrayList();
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof CustomerVO) {
					CustomerVO ov = (CustomerVO)newObjs[i];
					String pk_org = ov.getPk_org();
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String code = ov.getCode();
					String name = ov.getName();
					String shortName = ov.getShortname();
					int enableStatus = ov.getEnablestate();
					if(enableStatus==1)
						enableStatus=0;
					else if(enableStatus==2)
						enableStatus=1;
					else
						enableStatus=2;
					String buslicensenum = ov.getTaxpayerid();
					String legalbody = ov.getLegalbody();
					
					Map map = new HashMap();
//					map.put("id", id);
					map.put("code", code);
					map.put("name", name);
					map.put("orgCode", orgCode);
					map.put("orgName", orgName);
					map.put("shortName", shortName);
					map.put("enableStatus", enableStatus);
					map.put("buslicensenum", buslicensenum);
					map.put("legalbody", legalbody);
					map.put("syncTime",(new UFDate(System.currentTimeMillis())).toString());
					map.put("syncBy",InvocationInfoProxy.getInstance().getUserCode());
					map.put("srcSystemId", ov.getPk_customer());
					map.put("srcSystem", "NC");
					map.put("srcSystemName",name);
					map.put("srcSystemCode",code);		
					
					mapList.add(map);
				}
			}
			if(mapList!=null&&mapList.size()>0) {
//				String sessionID = WMSHttpTool.getsessionID();
//				hmap.put("lines", mapList);
//				Map map2 = new HashMap();
//				map2.put("params", hmap);
//				mapList2.add(map2);
				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/base/customer/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "bd_customer", "NC同步WMS客户");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "bd_customer", "NC同步WMS客户");
					throw new BusinessException(e.toString());
				}
			}
		}
	}

}
