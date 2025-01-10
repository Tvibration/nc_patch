package nc.itf.rest.wms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.framework.server.ISecurityTokenCallback;
import nc.bs.uapbd.tool.ProcessLogTools;
import nc.bs.wms.tool.WMSHttpTool;
import nc.itf.pubapp.pub.smart.IBillQueryService;

import nc.itf.uap.pf.IPFBusiAction;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pubitf.ic.location.ICLocationQuery;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.mes.returnvo;
import nc.vo.pu.m21.entity.OrderHeaderVO;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
//import uap.json.JSONArray;
import com.alibaba.fastjson.JSONArray;
//import uap.json.JSONObject;
import com.alibaba.fastjson.JSONObject;
import nc.vo.scmpub.api.rest.utils.RestUtils;

import java.util.Properties;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;

import org.json.JSONString;

import nc.jdbc.framework.processor.ColumnListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.pubapp.pattern.model.entity.bill.AbstractBill;
import nc.itf.ic.m4i.IGeneralOutMaintain;

public class RestForWMSimpl2 {
	
	private BaseDAO dao = new BaseDAO();
	private IPFBusiAction pfaction = (IPFBusiAction) NCLocator.getInstance().lookup(IPFBusiAction.class);
	private IBillQueryService IQ = (IBillQueryService) NCLocator.getInstance().lookup(IBillQueryService.class);
	private ICLocationQuery ILQ = (ICLocationQuery) NCLocator.getInstance().lookup(ICLocationQuery.class);  //货位查询服务接口
	
	public static String getValue(String key) {
	  	  Properties proper = null;
	    if (proper == null) {
	      try {
	        proper = new Properties();
	        proper.load(RestForWMSimpl.class.getClassLoader().getResourceAsStream("Wmsconfig.properties"));
	      } catch (Exception e) {
	        e.printStackTrace();
	        proper = null;
	        return null;		
	      }
	    }
	    return proper.getProperty(key);
	  }
	
	public JSONString SendWMS21(JSONObject jsonAy) {
		returnvo returnJson = new returnvo();
		String billcode = "";
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		List<Map> mapList = new ArrayList();
		List<Map> mapList2 = new ArrayList();
		try {
			String vbillcode = jsonAy.getString("vbillcode");  //采购订单号
			String pk = queryOne("select h.pk_order from po_order h where h.vbillcode='"+vbillcode+"' and h.dr=0");
			AbstractBill po_data = IQ.querySingleBillByPk(OrderVO.class, pk);
			if (po_data==null){
				throw new Exception("传输的采购订单ID在系统不存在或者已删除!");
			}
			OrderVO ov = (OrderVO)po_data;
			OrderHeaderVO ohv = ov.getHVO();
			OrderItemVO[] obvs = ov.getBVO();
			String pk_org = ohv.getPk_org();
			String creater = (String)dao.executeQuery("select u.user_code from sm_user u where u.cuserid='"+ohv.getCreator()+"'", new ColumnProcessor());  //创建人
			String wmsFlag = (String)dao.executeQuery("select s.value from pub_sysinit s where s.initcode = 'WMS' and s.pk_org='"+pk_org+"'", new ColumnProcessor());  //通过参数控制是否传WMS
			if(wmsFlag==null||wmsFlag.equals("N"))
				throw new Exception("采购订单所属组织在参数设置中设为不传WMS，请检查！");
			String orgCode = (String)dao.executeQuery("select o.code from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
			String orgName = (String)dao.executeQuery("select o.name from org_orgs o where o.pk_org='"+pk_org+"'", new ColumnProcessor());
			ohv.getVtrantypecode();
			Map map = new HashMap();
			map.put("srcSystemId", ohv.getPk_order());
			map.put("code", ohv.getVbillcode());
			map.put("billDate", ohv.getDbilldate().toString());
			map.put("delFlag", 0);
			map.put("traderSrcSystemId", ohv.getPk_supplier());
			map.put("orgSrcSystemId", ohv.getPk_org());
			map.put("remark", ohv.getVmemo());
			map.put("billTypeCode", ohv.getVtrantypecode());
			map.put("createBy", creater);
			map.put("createTime", ohv.getCreationtime().toString());
			for(int r=0;r<obvs.length;r++) {
				Map map_b = new HashMap();
				map_b.put("srcSystemId", obvs[r].getPk_order_b());
				map_b.put("lineNo", obvs[r].getCrowno());
				map_b.put("productSrcSystemId", obvs[r].getPk_material());
				map_b.put("orderCode", ohv.getVbillcode());
				map_b.put("batchCode", obvs[r].getVbatchcode());
				map_b.put("mainNum", obvs[r].getNnum().toString());
				map_b.put("astNum", obvs[r].getNastnum().toString());
				map_b.put("mainUnitSrcSystemId", obvs[r].getCunitid());
				map_b.put("astUnitSrcSystemId", obvs[r].getCastunitid());
				map_b.put("rate", obvs[r].getVchangerate());
				map_b.put("warehouseSrcSystemId", obvs[r].getPk_recvstordoc());
				map_b.put("remark", obvs[r].getVbmemo());
				map_b.put("delFlag", 0);
				map_b.put("metalPercent", obvs[r].getVbdef2());
				mapList2.add(map_b);
			}
			map.put("arriveDetailList", mapList2);
			mapList.add(map);
			
			if(mapList!=null&&mapList.size()>0) {

				JSONArray js = (JSONArray)JSONObject.toJSON(mapList);
				String jsons = js.toString();
				String url = WMSHttpTool.getWMSURL()+"/purchase/arrive-order/ds/save-batch-by-id";
				try {
					String res = WMSHttpTool.sendPost(jsons, url);
					if(res.contains("\"success\":true")) {
						NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.TRUE, url, res, "", jsons, mapList.get(0).get("srcSystemId").toString(), "po_order_b", "NC同步WMS采购订单");
					}else
						throw new BusinessException(res);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					NCLocator.getInstance().lookup(ProcessLogTools.class).insertLog_RequiresNew(UFBoolean.FALSE, url, "失败信息", e.getMessage(), jsons, mapList.get(0).get("srcSystemId").toString(), "po_order_b", "NC同步WMS采购订单");
					throw new BusinessException(e.toString());
				}
			}
			returnJson.setResultBillcode(billcode);
			returnJson.setReturnMessage("采购订单手动推送WMS成功！");
			returnJson.setStatus("1");
		} catch (Exception e) {
			returnJson.setResultBillcode("");
			returnJson.setStatus("0");
			returnJson.setReturnMessage(e.toString());
		}
		return RestUtils.toJSONString(returnJson);
	}
	
	public String queryOne(String sql) {
		BaseDAO dao = new BaseDAO();
		List<String> pk = null;
		try {
			pk = (List<String>) dao.executeQuery(sql, new ColumnListProcessor());
		} catch (DAOException e) {
			ExceptionUtils.wrappBusinessException("错误:" + e.getMessage());
		}
		if(pk==null||pk.size()==0){
		return null;
		}
		return pk.get(0);
	}
	
	public JSONString Delete4Y(JSONObject jsonAy) {
		returnvo returnJson = new returnvo();
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		List<Map> mapList = new ArrayList();
		List<Map> mapList2 = new ArrayList();
		try {
			String userID = jsonAy.getString("userID");  //用户ID
			String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+userID+"'";
 			String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
 			if(cuserid==null)
 				throw new Exception("制单人"+userID+"在NC不存在，请检查！");
			String cgeneralhid = jsonAy.getString("cgeneralhid");  //调拨出库单ID
			String vbillcode = queryOne("select h.vbillcode from ic_transout_h h where h.cgeneralhid='"+cgeneralhid+"'");			
			AbstractBill to_data = IQ.querySingleBillByPk(TransOutVO.class, cgeneralhid);
			if (to_data==null){
				throw new Exception("传输的调拨出库单ID在系统不存在或者已删除!");
			}
			
			dao.executeUpdate("delete ar_estirecbill h where h.pk_estirecbill in (select b.pk_estirecbill "
					+ "from ar_estirecitem b where b.top_billid=(select ih.cgeneralhid from ic_transout_h ih where ih.cgeneralhid='"+cgeneralhid+"' and ih.dr=0))");
			dao.executeUpdate("delete ar_estirecitem b where b.top_billid=(select ih.cgeneralhid from ic_transout_h ih where ih.cgeneralhid='"+cgeneralhid+"' and ih.dr=0)");
			dao.executeUpdate("delete to_presettleout where cbillid in (select b.cbillid from to_presettleout_b b inner join ic_transout_b tb on b.csrcbid=tb.cgeneralbid "
					+ "inner join ic_transout_h th on tb.cgeneralhid=th.cgeneralhid where th.cgeneralhid='"+cgeneralhid+"')");
			dao.executeUpdate("delete to_presettleout_b where cbill_bid in (select b.cbill_bid from to_presettleout_b b inner join ic_transout_b tb on b.csrcbid=tb.cgeneralbid "
					+ "inner join ic_transout_h th on tb.cgeneralhid=th.cgeneralhid where th.cgeneralhid='"+cgeneralhid+"')");
			dao.executeUpdate("update ia_ijbill set csrcmodulecode='IC' where cbillid in (select ib.cbillid from ia_ijbill_b ib inner join ic_transout_h ih on ib.csrcid=ih.cgeneralhid "
					+ "where ih.cgeneralhid='"+cgeneralhid+"' and ib.dr=0 and ih.dr=0)");
			TransOutVO tovo = (TransOutVO)to_data;
			InvocationInfoProxy.getInstance().setGroupId(tovo.getHead().getPk_group());  //设置集团环境变量
  			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量	
			if(tovo.getHead().getFbillflag()!=2)
				pfaction.processAction("CANCELSIGN", "4Y", null, tovo, null, null);
			pfaction.processAction("DELETE", "4Y", null, tovo, null, null);
			returnJson.setResultBillcode("");
			returnJson.setReturnMessage("调拨出库单"+tovo.getHead().getVbillcode()+"删除成功！");
			returnJson.setStatus("1");
		} catch (Exception e) {
			returnJson.setResultBillcode("");
			returnJson.setStatus("0");
			returnJson.setReturnMessage(e.toString());
		}
		return RestUtils.toJSONString(returnJson);
	}
	
	public JSONString Rewrite4I(JSONObject jsonAy) {

 		// TODO 自动生成的方法存根
 		returnvo returnJson = new returnvo();
 		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
 		try {
 			String userID = jsonAy.getString("userID");  //用户ID
			String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+userID+"'";
			String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
			if(cuserid==null)
				throw new Exception("制单人"+userID+"在NC不存在，请检查！");
			String approver = jsonAy.getString("approverID");  //审批人编码
			String sql_approver = "select u.cuserid from sm_user u where u.pk_psndoc='"+approver+"'";
			String approverid = (String) dao.executeQuery(sql_approver, new ColumnProcessor());
			if(approverid==null)
				throw new Exception("审批人"+approver+"在NC不存在，请检查！");
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			UFDate dmakedate_d = new UFDate(dmakedate);	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			
			String vnote = jsonAy.getString("vnote");  //备注
 			String successMessage = "";;
 			
 			String cgeneralhid = jsonAy.getString("cgeneralhid");  //出库申请单ID
 			String hpk = (String)dao.executeQuery("select b.cgeneralhid from ic_generalout_b b where b.csourcebillhid='"+cgeneralhid+"' and b.dr=0", new ColumnProcessor());
 			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
 			String WMSBillCode = jsonAy.getString("WMSBillCode");  //WMS单据号
 			GeneralOutVO GOVO_ori = IQ.querySingleBillByPk(GeneralOutVO.class, hpk);
 			if(GOVO_ori==null)
 				throw new Exception("其他出库单ID："+cgeneralhid+"在NC不存在，请检查！");
 			String pk_group = GOVO_ori.getParentVO().getPk_group();
 			
 			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
 			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
// 			InvocationInfoProxy.getInstance().setBizDateTime(audittime.getMillis());
 			int Fbillflag = GOVO_ori.getParentVO().getFbillflag();
 			if(Fbillflag!=2) {
 				GeneralOutVO[] tempvo = (GeneralOutVO[])pfaction.processAction("CANCELSIGN", "4I", null, GOVO_ori, null, null);
 				GOVO_ori = tempvo[0];
 			}
 			GeneralOutVO[] GIVO_ori2 = {GOVO_ori};
 			GeneralOutVO GIVO = (GeneralOutVO)GOVO_ori.clone();
 			GeneralOutVO[] GIVO2 = {GIVO};
 			GeneralOutBodyVO[] bvos = GIVO.getBodys();
 			GeneralOutHeadVO hvo = GIVO.getHead();
 			hvo.setVdef19(WMSBillCode);
 			hvo.setVnote(vnote);
 			hvo.setStatus(VOStatus.UPDATED);
 			JSONArray list = jsonAy.getJSONArray("list");
 			List<GeneralOutVO> bodylist = new ArrayList();
 			for(int i=0;i<list.size();i++){
 				String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid")==null?"null":list.getJSONObject(i).getString("cgeneralbid");  //出库申请表体主键 
 				cgeneralbid = (String)dao.executeQuery("select b.cgeneralbid from ic_generalout_b b where b.csourcebillbid='"+cgeneralbid+"' and b.dr=0", new ColumnProcessor());
 				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));  //入库主数量
 				UFDouble nassistnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));  //入库数量
 				String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode");
 				String cstateid = list.getJSONObject(i).getString("cstateid")==null?"":list.getJSONObject(i).getString("cstateid");
// 				String cworkcenterid = list.getJSONObject(i).getString("cworkcenterid");
 				for(int j=0;j<bvos.length;j++){
 					String cgeneralbid2 = bvos[j].getCgeneralbid();
 					if(cgeneralbid.equals(cgeneralbid2)){
 						HashMap<String,Object> matinfo = queryMaterialBD(bvos[j].getCmaterialoid());//查询物料单位重量
 						UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight"))); 						
 						bvos[j].setNnum(nnum);
 						bvos[j].setNassistnum(nassistnum);
 						bvos[j].setVbatchcode(vbatchcode);
 						bvos[j].setPk_batchcode(queryPK_batchcode(vbatchcode,bvos[j].getCmaterialoid()));
 						bvos[j].setCstateid(cstateid);
// 						bvos[j].setCworkcenterid(cworkcenterid);
 						if(!unitweight.equals(UFDouble.ZERO_DBL))
 							bvos[j].setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
 						bvos[j].setDproducedate(new UFDate(dmakedate));
 						bvos[j].setDvalidate(getDvalidate(bvos[j].getCmaterialoid(),hvo.getPk_org(),new UFDate(dmakedate)));
 						bvos[j].setStatus(VOStatus.UPDATED);			
 					}
 				}
 			}
 			
// 			FinProdInVO[] result = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, GIVO, null, null);
 			IGeneralOutMaintain MQ = (IGeneralOutMaintain) NCLocator.getInstance().lookup(IGeneralOutMaintain.class);
 			GeneralOutVO[] result = MQ.update(GIVO2,GIVO_ori2);
 			successMessage = "NC其他出库单保存";
 			if(sighFlag.equals("Y")){
 				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
 				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
 				pfaction.processAction("SIGN", "4I", null, result[0], null, null);
 				successMessage = successMessage+"并签字";
 			}
 			successMessage = successMessage+"成功！";

 			returnJson.setResultBillcode(result[0].getParentVO().getVbillcode());
 			returnJson.setReturnMessage(successMessage);
 			returnJson.setStatus("1");
 			
 		} catch (Exception e) {
 			// TODO 自动生成的 catch 块
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 		}
 		return RestUtils.toJSONString(returnJson);
 	
	}
	
	private UFDate getDvalidate(String wl,String pk_org,UFDate Dproducedate) throws Exception {
		Object bzq = dao.executeQuery("select (case when s.qualityunit=0 then s.qualitynum*365 else (case when s.qualityunit=1 then s.qualitynum*30 else s.qualitynum end) end) qualitynum from bd_materialstock s where s.pk_material='"+wl+"' and s.pk_org='"+pk_org+"' and s.dr=0",new ColumnProcessor());
		if(bzq==null) {
			String qualitymanflag = (String)dao.executeQuery("select s.qualitymanflag from bd_materialstock s where s.pk_material='"+wl+"' and s.pk_org='"+pk_org+"' and s.dr=0",new ColumnProcessor());
			if(qualitymanflag!=null&&qualitymanflag.equals("Y")) {
				String wlbm = (String)dao.executeQuery("select m.code from bd_material m where m.pk_material='"+wl+"'",new ColumnProcessor());
				throw new Exception("物料"+wlbm+"勾选了保质期管理却没有维护保质期，请检查！");
			}
			return new UFDate("9999-12-31");
		}
		else 
			return Dproducedate.getDateAfter(Integer.valueOf(bzq.toString()));
	}
	
	private HashMap<String,Object> queryMaterialBD(String pk_material) throws DAOException{
		String sql = "select unitweight from bd_material m where m.pk_material='"+pk_material+"'";
		HashMap<String,Object> result = (HashMap) dao.executeQuery(sql,new MapProcessor());  
		return result;
	}
 	
	private String queryPK_batchcode(String code,String materialPK) throws Exception{
		String sql ="select s.pk_batchcode from scm_batchcode s where s.vbatchcode='"+code+"' and s.cmaterialoid='"+materialPK+"' and s.dr=0";
		HashMap id = (HashMap) dao.executeQuery(sql,new MapProcessor());
		if (id!=null)
			return id.get("pk_batchcode").toString();
		else
			return null;
	}
}
