package nc.bs.wms.plugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import nc.bs.businessevent.BusinessEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.general.businessevent.ICGeneralCommonEvent;
import nc.bs.wms.tool.WMSHttpTool;
import nc.bs.uapbd.tool.HttpTool;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.vo.bd.material.MaterialVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmHeadVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.org.DeptVO;
import nc.vo.pu.m21.entity.OrderHeaderVO;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.jdbc.framework.processor.ColumnProcessor;

public class matoutAfterSign implements IBusinessListener {

	@Override
	public void doAction(IBusinessEvent arg0) throws BusinessException {
		// TODO 自动生成的方法存根
		if (arg0 instanceof ICGeneralCommonEvent) {
			ICGeneralCommonEvent be = (ICGeneralCommonEvent) arg0;
			Object newObjs = be.getOldObjs();
			List<Map> mapList = new ArrayList();
//			List<Map> mapList2 = new ArrayList();
			Map hmap = new HashMap();
			BaseDAO dao = new BaseDAO();
			dao.setAddTimeStamp(false);
			if (newObjs instanceof MaterialOutVO[]) {
				MaterialOutVO[] ovs = (MaterialOutVO[])newObjs;
				for(int i=0;i<ovs.length;i++) {
					  MaterialOutVO cv = ovs[i];
					  MaterialOutHeadVO phv = cv.getHead();
					  MaterialOutBodyVO[] pbv = (MaterialOutBodyVO[])cv.getChildrenVO();
					  String sourcetype = pbv[0].getCsourcetype();
//					  if(sourcetype==null||!sourcetype.equals("55A4"))
//						  continue;
					  String pk_org = phv.getPk_org();
					  String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					  String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
					  String Billmaker = (String)dao.executeQuery("select u.user_name from sm_user u where u.cuserid='"+phv.getBillmaker()+"'", new ColumnProcessor());
					  String billtypecode = (String)dao.executeQuery("select t.pk_billtypecode from bd_billtype t where t.pk_billtypeid='"+phv.getCtrantypeid()+"'", new ColumnProcessor());
					  Map map = new HashMap();
					  map.put("srcSystemId",phv.getCgeneralhid());
					  map.put("code",phv.getVbillcode());
					  map.put("billDate",phv.getDbilldate().toString());
					  map.put("delFlag",0);
					  map.put("orgIdSrcSystemId",pk_org);
					  map.put("orgCode",orgCode);
					  map.put("orgName",orgName);
					  map.put("type",billtypecode);
					  map.put("remark",phv.getVnote());
					  map.put("makingPersonName",Billmaker);
					  map.put("makingBillDate", phv.getDmakedate().toString());
					  map.put("inoutType",billtypecode);
					  map.put("warehouseSrcSystemId",phv.getCwarehouseid());
					  
					  List<Map> mapList2 = new ArrayList();
					  for(int j=0;j<pbv.length;j++) {
						  Map map_b = new HashMap();
						  map_b.put("srcSystemId", pbv[j].getCgeneralbid());
						  map_b.put("lineNo", pbv[j].getCrowno());
						  map_b.put("remark", pbv[j].getVnotebody());
						  map_b.put("productSrcSystemId", pbv[j].getCmaterialoid());
						  map_b.put("mainUnitSrcSystemId", pbv[j].getCunitid());
						  map_b.put("auxiliaryUnitSrcSystemId", pbv[j].getCastunitid());
						  map_b.put("planNum", pbv[j].getNassistnum().toString());
						  map_b.put("planMainNum", pbv[j].getNnum().toString());
						  map_b.put("conversionRate", pbv[j].getVchangerate());
						  map_b.put("delFlag", 0);
						  mapList2.add(map_b);
					  }
					  map.put("stockInoutDetailList", mapList2);
					  mapList.add(map);
				  }
				  if(mapList!=null&&mapList.size()>0) {
						JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
//						String jsons = js.toString().substring(1, js.toString().length()-1);
						String jsons = js.toString();
						String url = WMSHttpTool.getWMSURL()+"/stock/stock-inout-order/ds/save-batch-by-id";
						try {
							String res = WMSHttpTool.sendPost(jsons, url);
							if(res.contains("\"success\":true")) {
								NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "ic_material_h", "NC倒冲材料出库同步WMS");
							}else
								throw new BusinessException(res);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "ic_material_h", "NC倒冲材料出库同步WMS");
							throw new BusinessException(e.toString());
						}
					}	  
				
			}		
		}     
	
	}

}
