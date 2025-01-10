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
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.material.measdoc.MeasdocVO;
import nc.vo.bd.stordoc.StordocVO;
import nc.vo.org.DeptVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnProcessor;

public class measdocAfterEdit implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO Auto-generated method stub
		if (arg0 instanceof BDCommonEvent) {
			BDCommonEvent be = (BDCommonEvent) arg0;
			Object[] newObjs = be.getNewObjs();
			List<Map> mapList = new ArrayList();
			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			for (int i = 0; i < newObjs.length; i++) {
				if (newObjs[i] instanceof MeasdocVO) {
					MeasdocVO ov = (MeasdocVO)newObjs[i];
					String oppdimen = ov.getOppdimen();
					String pk_org = ov.getPk_org();
					String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					
					switch(oppdimen){
					  case "W":
						  oppdimen = "重量";
						  break;
					  case "L":
						  oppdimen = "长度";
						  break;
					  case "A":
						  oppdimen = "面积";
						  break;
					  case "V":
						  oppdimen = "体积";
						  break;
					  case "P":
						  oppdimen = "件数";
						  break;
					  case "T":
						  oppdimen = "时间";
						  break;
					  case "E":
						  oppdimen = "其他";
						  break;
					  }
					
					Map map = new HashMap();
//					map.put("id", id);
					map.put("code", ov.getCode());
					map.put("name", ov.getName());
					map.put("orgCode", orgCode);
					map.put("orgName", orgName);
					map.put("enableStatus", 1);
					map.put("typeName", oppdimen);
					map.put("syncTime",(new UFDate(System.currentTimeMillis())).toString());
//					map.put("create_time",(new UFDate(System.currentTimeMillis())).toString());
					map.put("syncBy",InvocationInfoProxy.getInstance().getUserCode());
					map.put("srcSystem", "NC");
					map.put("srcSystemId", ov.getPk_measdoc());
					map.put("srcSystemCode", ov.getCode());
					map.put("srcSystemName", ov.getName());
					mapList.add(map);
				}
			}
			if(mapList!=null&&mapList.size()>0) {

				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/base/productunit/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "bd_measdoc", "NC同步WMS计量单位");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "bd_measdoc", "NC同步WMS计量单位");
					throw new BusinessException(e.toString());
				}
			}
		}
	}

}
