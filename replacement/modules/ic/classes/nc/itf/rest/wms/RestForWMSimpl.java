package nc.itf.rest.wms;


import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import nc.bs.pub.pf.PfUtilTools;
import nc.itf.mmpac.pickm.IPickmBusinessService;
import nc.itf.mmpac.pickm.IPickmQueryService;
import nc.itf.mmpac.pmo.pac0002.IPMOBusinessService;
import nc.itf.mmpac.pmo.pac0002.IPMOQueryService;
import nc.itf.mmpac.wr.IWrBusinessService;
import nc.itf.mmpac.wr.pwr.IPwrMaintainService;
import nc.itf.pu.m21.IOrderQuery;
import nc.itf.pubapp.pub.smart.IBillQueryService;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.uap.pf.IPFBusiAction;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pubitf.ic.location.ICLocationQuery;
import nc.pubitf.ic.m45.api.IPurchaseInQueryAPI;
import nc.vo.ic.m45.entity.PurchaseInBodyVO;
import nc.vo.ic.m45.entity.PurchaseInHeadVO;
import nc.vo.ic.m45.entity.PurchaseInVO;
import nc.vo.ic.m46.entity.FinProdInBodyVO;
import nc.vo.ic.m46.entity.FinProdInHeadVO;
import nc.vo.ic.m46.entity.FinProdInVO;
import nc.vo.ic.m49.entity.BorrowInBodyVO;
import nc.vo.ic.m49.entity.BorrowInHeadVO;
import nc.vo.ic.m49.entity.BorrowInVO;
import nc.vo.ic.m4a.entity.GeneralInBodyVO;
import nc.vo.ic.m4a.entity.GeneralInHeadVO;
import nc.vo.ic.m4a.entity.GeneralInVO;
import nc.vo.ic.m4b.entity.ReturnInBodyVO;
import nc.vo.ic.m4b.entity.ReturnInVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4d.entity.MaterialOutHeadVO;
import nc.vo.ic.m4d.entity.MaterialOutVO;
import nc.vo.ic.m4e.entity.TransInBodyVO;
import nc.vo.ic.m4e.entity.TransInVO;
import nc.vo.ic.m4h.entity.BorrowOutBodyVO;
import nc.vo.ic.m4h.entity.BorrowOutHeadVO;
import nc.vo.ic.m4h.entity.BorrowOutVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4j.entity.ReturnOutBodyVO;
import nc.vo.ic.m4j.entity.ReturnOutVO;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.ic.m4k.entity.WhsTransBillVO;
import nc.vo.ic.m4y.entity.TransOutBodyVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.to.m5x.entity.BillVO;
import nc.vo.mes.returnvo;
import nc.vo.mmpac.pickm.entity.AggPickmVO;
import nc.vo.mmpac.pickm.entity.PickmItemVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOPlanOutputVO;
import nc.vo.mmpac.wr.entity.AggWrVO;
import nc.vo.mmpac.wr.entity.WrItemVO;
import nc.vo.pu.m21.entity.OrderItemVO;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pu.m23.entity.ArriveItemVO;
import nc.vo.pu.m23.entity.ArriveVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.qc.transtype.service.IArriveBillQuery;
//import uap.json.JSONArray;
import com.alibaba.fastjson.JSONArray;
//import uap.json.JSONObject;
import com.alibaba.fastjson.JSONObject;
import nc.vo.scmpub.api.rest.utils.RestUtils;

import java.util.Properties;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import com.google.common.collect.Lists;
import com.netflix.infix.lang.infix.antlr.EventFilterParser.null_predicate_return;

import org.json.JSONString;
import org.springframework.util.StringUtils;

import nc.jdbc.framework.generator.IdGenerator;
import nc.jdbc.framework.generator.SequenceGenerator;
import nc.jdbc.framework.processor.ArrayListProcessor;
import nc.jdbc.framework.processor.ColumnListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.vo.pubapp.pattern.model.entity.bill.AbstractBill;
import nc.itf.ic.m45.self.IPurchaseInMaintain;
import nc.itf.ic.m4d.IMaterialOutMaintain;

public class RestForWMSimpl {
	
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
	
	public JSONString Generate45(JSONObject jsonAy) {
		returnvo returnJson = new returnvo();
		PurchaseInVO[] result = null;
		String billcode = "";
		String successMessage = "";
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
		InvocationInfoProxy.getInstance().setUserId("1001A2100000001YCWTC");    //设置用户环境变量
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
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
			String vnote = jsonAy.getString("vnote");  //备注
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			String replenishflag = jsonAy.getString("replenishflag");  //是否退库
			UFBoolean replenishflag2 = UFBoolean.FALSE;
			if(replenishflag!=null&&replenishflag.equals("Y"))
				replenishflag2 = UFBoolean.TRUE;
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			UFDate dmakedate_d = dmakedate_t.getDate();	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
			String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
			
//			String billCodes = jsonAy.getString("billCodes");  //采购订单号数组
			
			String[] pks = null;
			ArrayList<String> pks_list = new ArrayList<String>();
			Map pks_map = new HashMap();
			for(int i=0;i<list.size();i++) {  //将单号转换为PK
				String pk = list.getJSONObject(i).getString("srcSystemId")==null?"null":list.getJSONObject(i).getString("srcSystemId");
				if(pks_map.get(pk)==null) {
					pks_list.add(pk);
					pks_map.put(pk, pk);
				}
					
			}
			pks = pks_list.toArray(new String[pks_list.size()]);
//			if (billCodes.length()>2){
//				billCodes = billCodes.substring(1, billCodes.length()-1);
//				pks = billCodes.split(",");
//				for(int j=0;j<pks.length;j++) {
//					
//				}
//			}else
//				throw new Exception("单据ID属性为空，请检查传输参数！");
			AbstractBill[] po_data = IQ.queryAbstractBillsByPks(OrderVO.class, pks);
				
			if (po_data==null||po_data[0]==null||po_data.length==0){
				throw new Exception("传输的采购订单ID在系统不存在或者已删除!");
			}
			
//			ArrayList<OrderItemVO> oi_newt = new ArrayList<OrderItemVO>();
//			UFDouble ntotalnum = UFDouble.ZERO_DBL;
//			for (int t=0;t<po_data.length;t++){
//				OrderItemVO[] oi = (OrderItemVO[])po_data[t].getChildrenVO();
//				for (int r=0;r<list.size();r++){
//					String srcSystemId_b = list.getJSONObject(r).getString("srcSystemId_b")==null?"null":list.getJSONObject(r).getString("srcSystemId_b");  //采购订单表体ID
//					UFDouble nnum = list.getJSONObject(r).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(r).getString("nnum"));  //到货主数量
//					ntotalnum = ntotalnum.add(nnum);
//					for (int s=0;s<oi.length;s++){
//						String Crowno=oi[s].getPk_order_b()==null?"null":oi[s].getPk_order_b();
//						if(Crowno.equals(srcSystemId_b)){
//							oi[s].setNcaninnum(nnum);
//							oi_newt.add(oi[s]);
//						}
//					}
//				}
//			}
//			po_data[0].setChildrenVO(oi_newt.toArray(new OrderItemVO[list.size()]));
//			OrderVO[] po_datas = {(OrderVO)po_data[0]};
			PurchaseInVO[] PV = (PurchaseInVO[])PfUtilTools.runChangeDataAry("21", "45", po_data);
			
			for (int c=0;c<PV.length;c++){
				PV[c].getParentVO().setCwarehouseid(cwarehouseid);
				PV[c].getParentVO().setVnote(vnote);
				PV[c].getParentVO().setCtrantypeid("0001A110000000002DXO");
				PV[c].getParentVO().setVtrantypecode("45-01");
//				PV[c].getParentVO().setNtotalnum(ntotalnum);
				PV[c].getHead().setFreplenishflag(replenishflag2);
				PV[c].getParentVO().setStatus(VOStatus.NEW);
//				PV[c].getParentVO().setVdef1("N");
				PurchaseInBodyVO[] bodys = PV[c].getBodys();
//				PurchaseInBodyVO[] newbodys = new PurchaseInBodyVO[bodys.length];  //重新组建表体VO
				ArrayList<PurchaseInBodyVO> oi_new = new ArrayList<PurchaseInBodyVO>();
				List<String> sqls = Lists.newArrayList();
				for (int i=0;i<list.size();i++){
					String pk_order_b = list.getJSONObject(i).getString("srcSystemId_b")==null?"null":list.getJSONObject(i).getString("srcSystemId_b");
					UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(i).getString("nnum"));
					if(replenishflag2==UFBoolean.TRUE&&nnum.compareTo(UFDouble.ZERO_DBL)>0)
						throw new Exception("退库数量必须是负数，请检查!");
					UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));
					String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode");
					String cprojectid = list.getJSONObject(i).getString("cprojectid")==null?"":list.getJSONObject(i).getString("cprojectid");  
					String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"null":list.getJSONObject(i).getString("clocationCode");  //货位编码
					String Locationid = GetLocationid(cwarehouseid,clocationCode);
//					String vskucode = list.getJSONObject(i).getString("vskucode")==null?"null":list.getJSONObject(i).getString("vskucode");  //特征码
//					String dproducedate = list.getJSONObject(i).getString("dproducedate")==null?"null":list.getJSONObject(i).getString("dproducedate");  //生产日期
					String cstateid = list.getJSONObject(i).getString("cstateid");  //库存状态ID
					//String dproducedate = jsonAy.getString("dproducedate");  //生产日期
					for (int j=0;j<bodys.length;j++){
						String sourcebillbid = bodys[j].getCsourcebillbid()==null?"null":bodys[j].getCsourcebillbid();
						String cfirstbillbid = bodys[j].getCfirstbillbid()==null?"null":bodys[j].getCfirstbillbid();  //源头单据表体ID
						String cfirstbillhid = InfoQuery(cfirstbillbid,"cfirstbillhid");  //重新查询获取源头单据表头ID
						String cgddh = InfoQuery(cfirstbillbid,"cgddh");  //重新查询获取源头单据号
						String notebody = bodys[j].getVnotebody()==null?"null":bodys[j].getVnotebody();	
						String clocationid = list.getJSONObject(i).getString("clocationid") == null ? "":list.getJSONObject(i).getString("clocationid");//货位
//						UFDate dproducedate_d = new UFDate(dproducedate);	
						if(pk_order_b.equals(sourcebillbid)){
//						if(pk_order_b.equals(sourcebillbid)){
//							sqls.add("update po_order_b set naccumstorenum=(select sum(pb.nnum) from ic_purchasein_b pb where pb.csourcebillbid='"+pk_order_b+"' and pb.dr=0) where pk_order_b='"+pk_order_b+"'");
							HashMap<String,Object> matinfo = queryMaterialBD(bodys[j].getCmaterialoid());//查询物料单位重量
							UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight")));
							HashMap<String,Object> poinfo = queryPo(pk_order_b);//查询可入库数量
							UFDouble krkzsl = poinfo.get("krkzsl")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(poinfo.get("krkzsl")));
							UFDouble krksl = poinfo.get("krksl")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(poinfo.get("krksl")));
					
							PurchaseInBodyVO newbody = (PurchaseInBodyVO)bodys[j].clone();
							newbody.setCrowno(String.valueOf(i)+"0");
							newbody.setNshouldnum(krkzsl);
							newbody.setNnum(nnum);
							newbody.setNshouldassistnum(krksl);
							newbody.setNassistnum(nastnum);
							newbody.setNqtunitnum(nastnum); 
							newbody.setVbatchcode(vbatchcode);
							newbody.setCprojectid(cprojectid);
							newbody.setVnotebody(".");
//							newbody.setVbdef3(InfoQuery(bodys[j].getCmaterialoid(),"cwfl"));
							newbody.setPk_batchcode(queryPK_batchcode(vbatchcode,bodys[j].getCmaterialoid()));
							newbody.setPk_creqwareid(cwarehouseid);
							newbody.setClocationid(Locationid);
							newbody.setClocationid(clocationid);
							newbody.setCfirstbillhid(cfirstbillhid);
							newbody.setVfirstbillcode(cgddh);
//							bodys[j].setCffileid(queryCffileid(vskucode,bodys[j].getCmaterialoid()));
							newbody.setDbizdate(dmakedate_d);
							newbody.setDproducedate(dmakedate_d);
							newbody.setDvalidate(getDvalidate(newbody.getCmaterialoid(),PV[c].getParentVO().getPk_org(),dmakedate_d));
							newbody.setCstateid(cstateid);
							UFDouble hsje=bodys[j].getNorigtaxprice().multiply(nnum).setScale(2, UFDouble.ROUND_HALF_UP);  //含税金额
//							UFDouble bhsje=bodys[j].getNorigprice().multiply(nnum).setScale(2, UFDouble.ROUND_HALF_UP);
							UFDouble bhsje=hsje.div(UFDouble.ONE_DBL.add(bodys[j].getNtaxrate().multiply(0.01))).setScale(2, UFDouble.ROUND_HALF_UP);   //无税金额    算法改为财务的那种倒推计算法
							newbody.setNorigtaxmny(hsje);
							newbody.setNtaxmny(hsje);
							newbody.setNcaltaxmny(bhsje);
							newbody.setNmny(bhsje);
							newbody.setNorigmny(bhsje);
							newbody.setNtax(hsje.sub(bhsje));
							newbody.setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
							newbody.setStatus(VOStatus.NEW);
							oi_new.add(newbody);
							break;
						}
					}
				}
				if(oi_new==null||oi_new.size()==0){
					throw new Exception("传输的采购订单表体记录都不满足入库条件,请检查！");
				}
				PV[c].setChildrenVO(oi_new.toArray(new PurchaseInBodyVO[oi_new.size()]));
				result = (PurchaseInVO[])pfaction.processAction("WRITE", "45", null, PV[c], null, null);
				billcode = result[0].getParentVO().getVbillcode();
				successMessage = "NC采购入库单保存";
				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "45", null, result[0], null, null);
					successMessage = successMessage+"并签字";
				}
				successMessage = successMessage+"成功！";
//				if(replenishflag2==UFBoolean.TRUE) {
//					for(String value : sqls){  //回写上游采购订单累计入库数量
//						dao.executeUpdate(value);
//					}
//				}
				
			}
			
			returnJson.setResultBillcode(billcode);
			returnJson.setReturnMessage(successMessage);
			returnJson.setStatus("1");
		} catch (Exception e) {
			returnJson.setResultBillcode("");
			returnJson.setStatus("0");
			returnJson.setReturnMessage(e.toString());
			if(result!=null&&result.length>0){
				String[] PurchaseInPKS = new String[]{result[0].getPrimaryKey()};
				IPurchaseInQueryAPI PQ = (IPurchaseInQueryAPI) NCLocator.getInstance().lookup(IPurchaseInQueryAPI.class);
                
				try {  //如果生成了采购入库，而审批异常，则删除采购入库单
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					PurchaseInVO[] newPurchaseInVO= (PurchaseInVO[])PQ.queryVOByIDs(PurchaseInPKS);
					pfaction.processAction("DELETE", "45", null, newPurchaseInVO[0], null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(returnJson);
	}
	
	public JSONString Generate4D(JSONObject jsonAy) {

		// TODO 自动生成的方法存根
		returnvo returnJson = new returnvo();
		MaterialOutVO[] result = null;
		String successMessage = "";
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
//			String company_code = jsonAy.getString("company_code");  //组织编码
//			String warehouseCode = jsonAy.getString("warehouseCode");  //仓库编码
//			String sql_ckid = "select s.pk_stordoc from bd_stordoc s inner join org_orgs o on s.pk_org=o.pk_org where s.code='"+warehouseCode+"' and o.code='"+company_code+"'";
//			String cwarehouseid = (String)dao.executeQuery(sql_ckid, new ColumnProcessor());
//			if(cwarehouseid==null)
//				throw new Exception("仓库编码"+warehouseCode+"在NC不存在，请检查！");
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
			String vnote = jsonAy.getString("vnote");  //备注
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
//			String replenishflag = jsonAy.getString("replenishflag");  //是否退库
//			UFBoolean replenishflag2 = UFBoolean.FALSE;
//			if(replenishflag!=null&&replenishflag.equals("Y"))
//				replenishflag2 = UFBoolean.TRUE;
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			UFDate dmakedate_d = dmakedate_t.getDate();	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
			JSONArray newMateriallist = jsonAy.getJSONArray("newMateriallist");  //获取表体记录
			String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			
			String cpickmid = jsonAy.getString("cpickmid");  //备料计划表头ID
			String hid = queryOne("select h.cpickmid from mm_pickm h where h.cpickmid='"+cpickmid+"' and h.dr=0");
			if (hid==null){
				throw new Exception("传输的备料计划ID("+cpickmid+")在NC系统中不存在或者已删除，请检查！");
			}
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
			IPickmQueryService IQ = (IPickmQueryService) NCLocator.getInstance().lookup(IPickmQueryService.class);
			AggPickmVO AP = IQ.querySingleBillByPk(cpickmid);
			String ResultBillcode = "";
			List<String> sqls = new ArrayList(); 
			Map rows_map = new HashMap();  //用来记录已推材料出库的无备料领料行号
			if(newMateriallist!=null&&newMateriallist.size()>0) {
				String maxno = queryOne("select to_char(max(to_number(b.vrowno))) vrowno from mm_pickm_b b where b.cpickmid='"+hid+"' and b.dr=0");
				insertNewMaterial(cwarehouseid,cpickmid,newMateriallist,dmakedate_d,maxno,rows_map);
			}
//			PickmItemVO[] apis = (PickmItemVO[])AP.getChildrenVO();
//			PickmItemVO[] apis_new = new PickmItemVO[list.size()];
			
//			for (int r=0;r<list.size();r++){
//				String cpickm_bid = list.getJSONObject(r).getString("cpickm_bid")==null?"null":list.getJSONObject(r).getString("cpickm_bid");
//				UFDouble nplanoutnum = list.getJSONObject(r).getString("nplanoutnum")==null?null:new UFDouble(list.getJSONObject(r).getString("nplanoutnum"));
//				UFDouble nplanoutastnum = list.getJSONObject(r).getString("nplanoutastnum")==null?null:new UFDouble(list.getJSONObject(r).getString("nplanoutastnum"));
//				for (int s=0;s<apis.length;s++){
//					String cpickm_bid2=apis[s].getCpickm_bid()==null?"null":apis[s].getCpickm_bid();
//					if(cpickm_bid2.equals(cpickm_bid)){
//						apis_new[r]=apis[s];
//						apis_new[r].setCoutstockid(cwarehouseid);
//						apis_new[r].setNoutableastnum(nplanoutastnum);
//						apis_new[r].setNoutablenum(nplanoutnum);
//					}
//				}
//			}
//			AP.setChildrenVO(apis_new)
			AP = IQ.querySingleBillByPk(cpickmid);
			AggPickmVO[] AP2 = {AP};
			MaterialOutVO[] PVS = (MaterialOutVO[])PfUtilTools.runChangeDataAry("55A3", "4D", AP2);  //因为有可能会分单，数据转换后变成多张材料出库单，结果集应该是数组
			Map pks_map = new HashMap();  //用来记录已推材料出库的无备料领料行，避免因分单而重复推单
			for(int i=0;i<PVS.length;i++){
				MaterialOutVO PV = PVS[i];
				PV.getParentVO().setCtrantypeid("0001A110000000002DYF");
				PV.getParentVO().setVtrantypecode("4D-01");
				PV.getParentVO().setCwarehouseid(cwarehouseid);
				PV.getParentVO().setVnote(vnote);
				PV.getParentVO().setVdef3("Y");
//				PV.getParentVO().setVdef2(wms_id);
				List<MaterialOutBodyVO> bodylist = new ArrayList();
				MaterialOutBodyVO[] bodys = (MaterialOutBodyVO[])PV.getChildrenVO();
//				MaterialOutBodyVO[] MBV = new MaterialOutBodyVO[bodys.length];
				if(list!=null&&list.size()>0) {
					for (int s=0;s<list.size();s++){
						String cpickm_bid = list.getJSONObject(s).getString("cpickm_bid")==null?"null":list.getJSONObject(s).getString("cpickm_bid");
						UFDouble nplanoutnum = list.getJSONObject(s).getString("nplanoutnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nplanoutnum"));
						UFDouble nplanoutastnum = list.getJSONObject(s).getString("nplanoutastnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nplanoutastnum"));
						String vbatchcode = list.getJSONObject(s).getString("vbatchcode")==null?"":list.getJSONObject(s).getString("vbatchcode");
						String cstateid = list.getJSONObject(s).getString("cstateid");
						String cworkcenterid = list.getJSONObject(s).getString("cworkcenterid");
						for (int t=0;t<bodys.length;t++){
							String sourcebillbid = bodys[t].getCsourcebillbid();
							String Vnotebody = bodys[t].getVnotebody()==null?"null":bodys[t].getVnotebody();
							String materialpk = bodys[t].getCmaterialoid();
							if(cpickm_bid.equals(sourcebillbid)){
//								bodys[t].setVnotebody("WMS生成");
								bodys[t].setVbatchcode(vbatchcode);
								bodys[t].setPk_batchcode(queryPK_batchcode(vbatchcode,materialpk));
//								bodys[t].setDbizdate(new UFDate(System.currentTimeMillis()));
								bodys[t].setDbizdate(dmakedate_d);
								bodys[t].setCstateid(cstateid);
								bodys[t].setCworkcenterid(cworkcenterid);
								HashMap ss = queryDvalidate(vbatchcode,materialpk);
								if(ss!=null){
									if(ss.get("dproducedate")!=null)
										bodys[t].setDproducedate(new UFDate(ss.get("dproducedate").toString()));
									if(ss.get("dvalidate")!=null)
										bodys[t].setDvalidate(new UFDate(ss.get("dvalidate").toString()));
								}
								
								MaterialOutBodyVO MBV=(MaterialOutBodyVO)bodys[t].clone();
								MBV.setCrowno(String.valueOf(s)+"0");
								MBV.setNassistnum(nplanoutastnum);
								MBV.setNnum(nplanoutnum);
								MBV.setNshouldassistnum(nplanoutastnum);
								MBV.setNshouldnum(nplanoutnum);
								MBV.setStatus(VOStatus.NEW);
								bodylist.add(MBV);
								break;
							}
							
						}
					}
				}
				if(newMateriallist!=null&&newMateriallist.size()>0){
					for (int t=0;t<newMateriallist.size();t++){
						String pk_material = newMateriallist.getJSONObject(t).getString("pk_material")==null?"null":newMateriallist.getJSONObject(t).getString("pk_material");
						UFDouble nplanoutnum = newMateriallist.getJSONObject(t).getString("nplanoutnum")==null?null:new UFDouble(newMateriallist.getJSONObject(t).getString("nplanoutnum"));
						UFDouble nplanoutastnum = newMateriallist.getJSONObject(t).getString("nplanoutastnum")==null?null:new UFDouble(newMateriallist.getJSONObject(t).getString("nplanoutastnum"));
						String vbatchcode = newMateriallist.getJSONObject(t).getString("vbatchcode")==null?"":newMateriallist.getJSONObject(t).getString("vbatchcode");
						String cstateid = newMateriallist.getJSONObject(t).getString("cstateid");
						String cworkcenterid = newMateriallist.getJSONObject(t).getString("cworkcenterid");
						for (int v=0;v<bodys.length;v++){
							String Cmaterialoid = bodys[v].getCmaterialoid();
							String Csourcebillbid = bodys[v].getCsourcebillbid();
							String cbombid = queryOne("select b.cbombid from mm_pickm_b b where b.cpickm_bid='"+bodys[v].getCpickmbid()+"'");
							String vrowno = queryOne("select b.vrowno from mm_pickm_b b where b.cpickm_bid='"+bodys[v].getCpickmbid()+"'");
							if(pk_material.endsWith(Cmaterialoid)&&cbombid==null) {
								Object pk = pks_map.get(Csourcebillbid);
								if(pk!=null)
									continue;
								Object row = rows_map.get(vrowno);
								if(row==null)
									continue;
								bodys[v].setVbatchcode(vbatchcode);
								bodys[v].setPk_batchcode(queryPK_batchcode(vbatchcode,Cmaterialoid));
//								bodys[v].setDbizdate(dmakedate_d);
								bodys[v].setCstateid(cstateid);
								bodys[v].setCworkcenterid(cworkcenterid);
								sqls.add("update mm_pickm_b b set b.nshouldastnum=0,b.nshouldnum=0 where b.cpickm_bid='"+bodys[v].getCpickmbid()+"'");
								HashMap ss = queryDvalidate(vbatchcode,Cmaterialoid);
								if(ss!=null){
									if(ss.get("dproducedate")!=null)
										bodys[v].setDproducedate(new UFDate(ss.get("dproducedate").toString()));
									if(ss.get("dvalidate")!=null)
										bodys[v].setDvalidate(new UFDate(ss.get("dvalidate").toString()));
								}
								
								MaterialOutBodyVO MBV=(MaterialOutBodyVO)bodys[v].clone();
								MBV.setCrowno(String.valueOf((list==null?0:list.size())+t+1)+"0");
								MBV.setNassistnum(nplanoutastnum);
								MBV.setNnum(nplanoutnum);
								MBV.setNshouldassistnum(nplanoutastnum);
								MBV.setNshouldnum(nplanoutnum);
								MBV.setStatus(VOStatus.NEW);
								bodylist.add(MBV);
								pks_map.put(Csourcebillbid, Csourcebillbid);
								break;
							}
						}
					}
				}
				if(bodylist==null||bodylist.size()==0)
					continue;
				//newMateriallist
				PV.setChildrenVO(bodylist.toArray(new MaterialOutBodyVO[bodylist.size()]));
				
				result = (MaterialOutVO[])pfaction.processAction("WRITE", "4D", null, PV, null, null);
				if(ResultBillcode.equals(""))
					ResultBillcode = result[0].getParentVO().getVbillcode();
				else
					ResultBillcode = ResultBillcode+","+result[0].getParentVO().getVbillcode();
				
				successMessage = "NC材料出库单保存";
				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "4D", null, result[0], null, null);
					successMessage = successMessage+"并签字";
				}
				successMessage = successMessage+"成功！";
				for(int j=0;j<sqls.size();j++) {
					dao.executeUpdate(sqls.get(j));
				}
			}
			
			returnJson.setResultBillcode(ResultBillcode);
			returnJson.setReturnMessage(successMessage);
			returnJson.setStatus("1");
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			returnJson.setResultBillcode("");
			returnJson.setStatus("0");
			returnJson.setReturnMessage(e.toString());
			if(result!=null&&result.length>0){
				String pk = result[0].getParentVO().getCgeneralhid();
				MaterialOutVO errorVo = IQ.querySingleBillByPk(MaterialOutVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4D", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(returnJson);
	}
	
	public JSONString Generate4D_2(JSONObject jsonAy) {
		// TODO 自动生成的方法存根
		returnvo returnJson = new returnvo();
		String successMessage = "";
		MaterialOutVO[] result = null;
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			try {
				String userID = jsonAy.getString("userID");  //制单人ID
				String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+userID+"'";
				String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
				if(cuserid==null)
					throw new Exception("制单人"+userID+"在NC不存在，请检查！");
				String approver = jsonAy.getString("approverID");  //审批人ID
				String sql_approver = "select u.cuserid from sm_user u where u.pk_psndoc='"+approver+"'";
				String approverid = (String) dao.executeQuery(sql_approver, new ColumnProcessor());
				if(approverid==null)
					throw new Exception("审批人"+approver+"在NC不存在，请检查！");
				String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
				String vnote = jsonAy.getString("vnote");  //备注
				String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识

				String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
				UFDate dmakedate_d = dmakedate_t.getDate();	
				InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
				JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
				String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
				String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
				String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
				String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
				String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
				String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
				
//				String ctrantypeCode = jsonAy.getString("ctrantypeCode");  //出入库类型编码
				String cbizid = jsonAy.getString("cbizid");  //领料员ID
				String cwhsmanagerid = jsonAy.getString("cwhsmanagerid");  //库管员ID
				String pk_dept = jsonAy.getString("pk_dept");  //领料部门ID
				String pk_dept_v = queryOne("select v.pk_vid from org_dept_v v where v.pk_dept='"+pk_dept+"'");
//				String sql_trantype = "select t.pk_billtypeid from bd_billtype t where t.pk_billtypecode='"+ctrantypeCode+"' and t.pk_group='"+groupId+"'";
//				String ctrantypeid = (String) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql_trantype, new ColumnProcessor());
				InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
				InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
				MaterialOutVO mvo = new MaterialOutVO();
				MaterialOutHeadVO hvo= new MaterialOutHeadVO();
				
				hvo.setCwarehouseid(cwarehouseid);
				hvo.setCbizid(cbizid);
				hvo.setCdptid(pk_dept);
				hvo.setCdptvid(pk_dept_v);
				hvo.setCdrawcalbodyoid(pk_org);
				hvo.setCdrawcalbodyvid(pk_org_v);
				hvo.setCdrawwarehouseid(cwarehouseid);
				hvo.setCorpoid(pk_org);
				hvo.setCorpvid(pk_org_v);
				hvo.setCtrantypeid("0001A110000000002DYF");
				hvo.setCwhsmanagerid(cwhsmanagerid);
				hvo.setPk_org(pk_org);
				hvo.setPk_org_v(pk_org_v);
				hvo.setVtrantypecode("4D-01");
				hvo.setVdef3("Y");
				hvo.setVnote(vnote);
				hvo.setStatus(VOStatus.NEW);
				mvo.setParentVO(hvo);
				MaterialOutBodyVO[] MBV = new MaterialOutBodyVO[list.size()];
				for (int s=0;s<list.size();s++){
					String cmaterialoid = list.getJSONObject(s).getString("cmaterialoid");  //物料id
					String ccostobject = list.getJSONObject(s).getString("ccostobject");  //成本对象（产成品ID）
//					String cprodprojectid = list.getJSONObject(s).getString("cprodprojectid");  //产成品辅助属性-项目
//					String cprojectid = list.getJSONObject(s).getString("cprojectid");  //项目
//					String cworkcenterid = list.getJSONObject(s).getString("cworkcenterid");  //工作中心
					String vbatchcode = list.getJSONObject(s).getString("vbatchcode");  //批次号
//					String clocationCode = list.getJSONObject(s).getString("clocationCode")==null?"null":list.getJSONObject(s).getString("clocationCode");  //货位编码
//					String clocationid = GetLocationid(cwarehouseid,clocationCode);
					String vproductbatch = list.getJSONObject(s).getString("vproductbatch");  //生产订单号
					String cstateid = list.getJSONObject(s).getString("cstateid");  //库存状态
					String cworkcenterid = list.getJSONObject(s).getString("cworkcenterid");  //工作中心
//					String vskucode = list.getJSONObject(s).getString("vskucode")==null?"":list.getJSONObject(s).getString("vskucode");  //特征码
					UFDouble nnum = list.getJSONObject(s).getString("nnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nnum"));   //主数量
					UFDouble nassistnum = list.getJSONObject(s).getString("nassistnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nassistnum"));	//辅数量	
					String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
							+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
					HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
					String castunitid = "";
					String pk_source = "";
					String zdw = "";
					String measrate = "";
					if(materialMap!=null&&materialMap.get("zdw")!=null){
						castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
						pk_source=materialMap.get("pk_source").toString();
						zdw=materialMap.get("zdw").toString();
						measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
					}else
						throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
//					MaterialOutBodyVO bvo = MBV[s];
					MaterialOutBodyVO bvo = new MaterialOutBodyVO();
					bvo.setBassetcard(UFBoolean.FALSE);
					bvo.setBbarcodeclose(UFBoolean.FALSE);
					bvo.setBcseal(UFBoolean.FALSE);
					bvo.setBonroadflag(UFBoolean.FALSE);
					bvo.setBreworkflag(UFBoolean.FALSE);
					bvo.setCastunitid(castunitid);
					bvo.setCbodytranstypecode("4D-01");
					bvo.setCbodywarehouseid(cwarehouseid);
					bvo.setCmaterialoid(cmaterialoid);
					bvo.setCmaterialvid(pk_source);
					bvo.setCorpoid(pk_org);
					bvo.setCorpvid(pk_org_v);
					bvo.setCcostobject(ccostobject);
					bvo.setCrowno(String.valueOf(s)+"0");
					bvo.setCunitid(zdw);
					bvo.setNassistnum(nassistnum);
					bvo.setNnum(nnum);
					bvo.setNshouldassistnum(nassistnum);
					bvo.setNshouldnum(nnum);
					bvo.setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
					bvo.setPk_group(pk_group);
					bvo.setPk_org(pk_org);
					bvo.setPk_org_v(pk_org_v);
					bvo.setVbatchcode(vbatchcode);
					bvo.setVchangerate(measrate);
					bvo.setVproductbatch(vproductbatch);
					bvo.setCstateid(cstateid);
					bvo.setDproducedate(dmakedate_d);
					bvo.setDbizdate(dmakedate_d);
					bvo.setDvalidate(getDvalidate(cmaterialoid,pk_org,dmakedate_d));
					MBV[s] = bvo;
					MBV[s].setStatus(VOStatus.NEW);
				}

				mvo.setChildrenVO(MBV);
				result = (MaterialOutVO[])pfaction.processAction("WRITE", "4D", null, mvo, null, null);
				successMessage = "NC材料出库单保存";
				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					MaterialOutVO[] result2 = (MaterialOutVO[])pfaction.processAction("SIGN", "4D", null, result[0], null, null);
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
				if(result!=null&&result.length>0){
					String pk = result[0].getParentVO().getCgeneralhid();
					MaterialOutVO errorVo = IQ.querySingleBillByPk(MaterialOutVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "4D", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(returnJson);
	}
	
	public JSONString Rewrite4D(JSONObject jsonAy) {
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
			UFDate dmakedate_d = dmakedate_t.getDate();	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			
			String vnote = jsonAy.getString("vnote");  //备注
 			String successMessage = "";;
 			
 			String cgeneralhid = jsonAy.getString("cgeneralhid");  //材料出库单ID
 			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
 			MaterialOutVO MOVO_ori = IQ.querySingleBillByPk(MaterialOutVO.class, cgeneralhid);
 			if(MOVO_ori==null)
 				throw new Exception("材料出库单ID："+cgeneralhid+"在NC不存在，请检查！");
 			String pk_group = MOVO_ori.getParentVO().getPk_group();
 			
 			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
 			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
// 			InvocationInfoProxy.getInstance().setBizDateTime(audittime.getMillis());
 			int Fbillflag = MOVO_ori.getParentVO().getFbillflag();
 			if(Fbillflag!=2) {
 				MaterialOutVO[] tempvo = (MaterialOutVO[])pfaction.processAction("CANCELSIGN", "45", null, MOVO_ori, null, null);
 				MOVO_ori = tempvo[0];
 			}
 			MaterialOutVO[] GIVO_ori2 = {MOVO_ori};
 			MaterialOutVO GIVO = (MaterialOutVO)MOVO_ori.clone();
 			MaterialOutVO[] GIVO2 = {GIVO};
 			MaterialOutBodyVO[] bvos = GIVO.getBodys();
 			int rowcount = bvos.length;
 			MaterialOutHeadVO hvo = GIVO.getHead();
 			hvo.setVdef3("Y");
 			hvo.setVnote(vnote);
 			hvo.setStatus(VOStatus.UPDATED);
 			JSONArray list = jsonAy.getJSONArray("list");
 			List<MaterialOutBodyVO> bodylist = new ArrayList();
 			Map<String,String> rows_map = new HashMap<String,String>();  //用来记录是否多次传同个ID,以便判断是修改还是增行
 			for(int i=0;i<list.size();i++){
 				String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid")==null?"null":list.getJSONObject(i).getString("cgeneralbid");  //表体主键 				
 				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));  //入库主数量
 				UFDouble nassistnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));  //入库数量
 				String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode");
 				String cstateid = list.getJSONObject(i).getString("cstateid")==null?"":list.getJSONObject(i).getString("cstateid");
 				String cworkcenterid = list.getJSONObject(i).getString("cworkcenterid");
 				for(int j=0;j<bvos.length;j++){
 					String cgeneralbid2 = bvos[j].getCgeneralbid();
 					if(cgeneralbid.equals(cgeneralbid2)){
// 						HashMap<String,Object> matinfo = queryMaterialBD(bvos[j].getCmaterialoid());//查询物料单位重量
// 						UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight"))); 
// 						MaterialOutBodyVO tabvo = (MaterialOutBodyVO)bvos[j].clone();
// 						tabvo.setStatus(VOStatus.NEW);
// 						bvos[j].setStatus(VOStatus.DELETED);
// 						tabvo.setNnum(nnum);
// 						tabvo.setNassistnum(nassistnum);
// 						tabvo.setVbatchcode(vbatchcode);
// 						tabvo.setPk_batchcode(queryPK_batchcode(vbatchcode,tabvo.getCmaterialoid()));
// 						tabvo.setCstateid(cstateid);
// 						tabvo.setCworkcenterid(cworkcenterid);
// 						tabvo.setDbizdate(dmakedate_d);
// 						int l = 20-String.valueOf(rowcount).length()-3;
// 						rowcount = rowcount+1;
// 						tabvo.setCgeneralbid(tabvo.getCgeneralbid().substring(0, l)+"DS"+String.valueOf(rowcount)+"0");
// 						tabvo.setCrowno(String.valueOf(rowcount)+"0");
// 						if(!unitweight.equals(UFDouble.ZERO_DBL))
// 							tabvo.setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
// 						bodylist.add(tabvo);
 						
 						MaterialOutBodyVO tabvo = null;
 						String hadsendflag = (String)rows_map.get(cgeneralbid);
 						if(hadsendflag!=null) {
 							for(int k=0;k<bvos.length;k++) {
 								String hadsendflag2 = (String)rows_map.get(bvos[k].getCgeneralbid());
 								if(hadsendflag2!=null)
										continue;
 								if(bvos[j].getCmaterialoid().equals(bvos[k].getCmaterialoid())) {
 									tabvo = bvos[k];
 		 							tabvo.setStatus(VOStatus.UPDATED);
 		 							rows_map.put(bvos[k].getCgeneralbid(), bvos[k].getCgeneralbid());
 		 							break;
 								}
 							}
 							if(tabvo==null) {
 								tabvo = (MaterialOutBodyVO)bvos[j].clone();
		 						tabvo.setStatus(VOStatus.NEW);
		 						rowcount = rowcount+1;
		 						tabvo.setCrowno(String.valueOf(rowcount)+"0");
		 						int l = 20-String.valueOf(rowcount).length()-3;
		 						IdGenerator idGenerator = new SequenceGenerator();
		 						tabvo.setCgeneralbid(idGenerator.generate());

 							}
 							
 						}else {
 							tabvo = bvos[j];
 							tabvo.setStatus(VOStatus.UPDATED);
 							rows_map.put(cgeneralbid, cgeneralbid);
 						}
 						HashMap<String,Object> matinfo = queryMaterialBD(tabvo.getCmaterialoid());//查询物料单位重量
 						UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight"))); 						
 						tabvo.setNnum(nnum);
 						tabvo.setNassistnum(nassistnum);
 						tabvo.setVbatchcode(vbatchcode);
 						tabvo.setPk_batchcode(queryPK_batchcode(vbatchcode,bvos[j].getCmaterialoid()));
 						tabvo.setCstateid(cstateid);
 						tabvo.setCworkcenterid(cworkcenterid);
 						tabvo.setDbizdate(dmakedate_d);
 						tabvo.setDproducedate(dmakedate_d);
 						tabvo.setDvalidate(getDvalidate(tabvo.getCmaterialoid(),hvo.getPk_org(),dmakedate_d));
 						if(!unitweight.equals(UFDouble.ZERO_DBL))
 							tabvo.setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
 						bodylist.add(tabvo);
 					}
 				}
 			}
// 			for(int s=0;s<bvos.length;s++){
// 				String cgeneralbid = bvos[s].getCgeneralbid();
// 				String crowno = bvos[s].getCrowno();
// 				if(rows_map.get(cgeneralbid)==null) {
// 					bvos[s].setStatus(VOStatus.DELETED);
// 					bodylist.add(bvos[s]);
// 				}
// 			}
 			GIVO.setChildrenVO(bodylist.toArray(new MaterialOutBodyVO[bodylist.size()]));
 			
// 			FinProdInVO[] result = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, GIVO, null, null);
 			IMaterialOutMaintain MQ = (IMaterialOutMaintain) NCLocator.getInstance().lookup(IMaterialOutMaintain.class);
 			MaterialOutVO[] result = MQ.update(GIVO2,GIVO_ori2);
 			successMessage = "NC材料出库单保存";
 			if(sighFlag.equals("Y")){
 				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
 				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
 				pfaction.processAction("SIGN", "45", null, result[0], null, null);
 				successMessage = successMessage+"并签字";
 			}
 			successMessage = successMessage+"成功！";
 			dao.executeUpdate("update ic_material_h h set h.dbilldate='"+dmakedate+"' where h.cgeneralhid='"+cgeneralhid+"'");

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
	
	public void insertNewMaterial(String cwarehouseid,String cpickmid,JSONArray newMateriallist,UFDate dmakedate_d,String maxno,Map rows_map) throws Exception{
		IPickmBusinessService IPS = (IPickmBusinessService) NCLocator.getInstance().lookup(IPickmBusinessService.class);
		List<PickmItemVO> bodylist = new ArrayList();
		List<String> sqls = new ArrayList();
		String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
		String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
		String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
		String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
		for(int i=0;i<newMateriallist.size();i++) {
			PickmItemVO pv = new PickmItemVO();
			String pk_material = newMateriallist.getJSONObject(i).getString("pk_material")==null?"null":newMateriallist.getJSONObject(i).getString("pk_material");
			UFDouble nplanoutnum = newMateriallist.getJSONObject(i).getString("nplanoutnum")==null?null:new UFDouble(newMateriallist.getJSONObject(i).getString("nplanoutnum"));
			UFDouble nplanoutastnum = newMateriallist.getJSONObject(i).getString("nplanoutastnum")==null?null:new UFDouble(newMateriallist.getJSONObject(i).getString("nplanoutastnum"));
			String vbatchcode = newMateriallist.getJSONObject(i).getString("vbatchcode")==null?"null":newMateriallist.getJSONObject(i).getString("vbatchcode");
			String cstateid = newMateriallist.getJSONObject(i).getString("cstateid")==null?"null":newMateriallist.getJSONObject(i).getString("cstateid");
			HashMap materialMap = getmaterialInfo(pk_material);
			String castunitid = "";
			String pk_source = "";
			String zdw = "";
			String measrate = "";
			if(materialMap!=null&&materialMap.get("zdw")!=null){
				castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
				pk_source=materialMap.get("pk_source").toString();
				zdw=materialMap.get("zdw").toString();
				measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
			}else
				throw new Exception("物料主键"+pk_material+"在NC系统中不存在！");
			
			pv.setStatus(VOStatus.NEW);
			pv.setBautobuilt(UFBoolean.FALSE);
			pv.setBcandeliver(UFBoolean.TRUE);
			pv.setBcanreplace(UFBoolean.FALSE);
			pv.setBcontroll(UFBoolean.FALSE);
			pv.setBcustomermaterial(UFBoolean.FALSE);
			pv.setBkitmaterial(UFBoolean.FALSE);
			pv.setBmainmaterial(UFBoolean.FALSE);
			pv.setBprojectmaterial(UFBoolean.FALSE);
			pv.setBunplannpicking(UFBoolean.TRUE);
			pv.setBupwardround(UFBoolean.FALSE);
			pv.setCbastunitid(castunitid);
			pv.setCbmaterialid(pk_material);
			pv.setCbmaterialvid(pk_source);
			pv.setCbunitid(zdw);
			pv.setCdeliverorgid(pk_org);
			pv.setCdeliverorgvid(pk_org_v);
			pv.setCoutstockid(cwarehouseid);
			pv.setCpickmid(cpickmid);
			pv.setDrequiredate(dmakedate_d);
			pv.setFbackflushtype(1);
			pv.setFitemsource(0);
			pv.setFitemtype(0);
			pv.setFreplaceinfo(1);
			pv.setFsupplytype(0);
			pv.setNaccoutastnum(UFDouble.ZERO_DBL);
			pv.setNaccoutnum(UFDouble.ZERO_DBL);
			pv.setNdissipationum(new UFDouble("0"));
			pv.setNplanoutastnum(nplanoutastnum);
			pv.setNplanoutnum(nplanoutnum);
			pv.setNquotastnum(UFDouble.ONE_DBL);
			pv.setNquotnum(UFDouble.ONE_DBL);
			pv.setNshouldastnum(UFDouble.ZERO_DBL);
			pv.setNshouldnum(UFDouble.ZERO_DBL);
			pv.setNunitastnum(UFDouble.ONE_DBL);
			pv.setNunitnum(UFDouble.ONE_DBL);
			pv.setNunituseastnum(nplanoutastnum);
			pv.setNunitusenum(nplanoutnum);
			pv.setPk_group(pk_group);
			pv.setPk_org(pk_org);
			pv.setPk_org_v(pk_org_v);
			pv.setVbchangerate(measrate);
			String rowno = String.valueOf(Integer.valueOf(maxno)+(i+1)*10);
			pv.setVrowno(rowno);
			rows_map.put(rowno, rowno);
			bodylist.add(pv);
			sqls.add("delete ic_material_h where cgeneralhid in (select b.cgeneralhid from ic_material_b b inner join ic_material_h h on b.cgeneralhid=h.cgeneralhid where b.csourcebillhid='"+cpickmid+"' and b.cmaterialoid='"+pk_material+"' and b.nnum is null and b.dr=0 and h.fbillflag=2)");
			sqls.add("delete ic_material_b where cgeneralhid in (select b.cgeneralhid from ic_material_b b inner join ic_material_h h on b.cgeneralhid=h.cgeneralhid where b.csourcebillhid='"+cpickmid+"' and b.cmaterialoid='"+pk_material+"' and b.nnum is null and b.dr=0 and h.fbillflag=2)");
		}
		IPS.noItemsDeliverMaterial(bodylist.toArray(new PickmItemVO[bodylist.size()]));
		for(int j=0;j<sqls.size();j++) {
			dao.executeUpdate(sqls.get(j));
		}
	}
	
	public HashMap getmaterialInfo(String wl) throws DAOException {
		String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
				+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+wl+"'";
		HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
		return materialMap;
	}
	
	public JSONString Generate4I(JSONObject jsonAy) {
		returnvo returnJson = new returnvo();
		GeneralOutVO[] result = null;
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		try {
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			GeneralOutVO GOVO = new GeneralOutVO();
			GeneralOutHeadVO hvo = new GeneralOutHeadVO();
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //入库仓库id

			String vnote = jsonAy.getString("vnote");  //表头备注
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			String cwhsmanagerid = jsonAy.getString("cwhsmanagerid");  //库管员id
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			UFDate dmakedate_d = dmakedate_t.getDate();	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		    String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
			String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
			
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
			String pk_dept = jsonAy.getString("pk_dept");  //部门ID
			String pk_dept_v = queryOne("select v.pk_vid from org_dept_v v where v.pk_dept='"+pk_dept+"'");
			JSONArray list = jsonAy.getJSONArray("list");
			GeneralOutBodyVO[] bvos = new GeneralOutBodyVO[list.size()];
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
			String successMessage = "";
			String ctrantypeid = jsonAy.getString("ctrantypeid");  //出入库类型ID
			if(ctrantypeid==null||ctrantypeid.equals(""))
				ctrantypeid = "0001A110000000002DYI";
			String vtrantypecode = queryOne("select t.pk_billtypecode from bd_billtype t where t.pk_billtypeid='"+ctrantypeid+"'");
			
			hvo.setCorpoid(pk_org);
			hvo.setCorpvid(pk_org_v);
			hvo.setCtrantypeid(ctrantypeid);
			hvo.setCwarehouseid(cwarehouseid);
			hvo.setCwhsmanagerid(cwhsmanagerid);
			hvo.setFbillflag(2);
			hvo.setIprintcount(0);
			hvo.setPk_group(pk_group);
			hvo.setPk_org(pk_org);
			hvo.setPk_org_v(pk_org_v);
			hvo.setVnote(vnote);
			hvo.setVtrantypecode(vtrantypecode);
			hvo.setVdef20("Y");
			hvo.setCdptid(pk_dept);
			hvo.setCdptvid(pk_dept_v);
			hvo.setStatus(VOStatus.NEW);
			for(int i=0;i<list.size();i++){
				String cmaterialoid = list.getJSONObject(i).getString("cmaterialoid");  //物料id
//				String clocationCode = list.getJSONObject(i).getString("clocationCode");  //货位编码
				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));   //主数量
				UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));	//辅数量			
				String vbatchcode = list.getJSONObject(i).getString("vbatchcode");  //批次号
				String cstateid = list.getJSONObject(i).getString("cstateid");
				String clocationid = list.getJSONObject(i).getString("clocationid") == null ? "" : list.getJSONObject(i).getString("clocationid");//货位
//				String vskucode = list.getJSONObject(i).getString("vskucode")==null?"null":list.getJSONObject(i).getString("vskucode");  //特征码
//				String cprojectid = list.getJSONObject(i).getString("cprojectid")==null?"":list.getJSONObject(i).getString("cprojectid");  //项目id
//				String Locationid = GetLocationid(cwarehouseid,clocationCode);
				String sql_mv = "select v.pk_source from bd_material_v v where v.pk_material='"+cmaterialoid+"'";
				
				String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
						+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
				HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
				String castunitid = "";
				String pk_source = "";
				String zdw = "";
				String measrate = "";
				if(materialMap!=null&&materialMap.get("zdw")!=null){
					castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
					pk_source=materialMap.get("pk_source").toString();
					zdw=materialMap.get("zdw").toString();
					measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
				}else
					throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
				
				HashMap<String,Object> matinfo = queryMaterialBD(cmaterialoid);//查询物料单位重量
				UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight")));
				bvos[i] = new GeneralOutBodyVO();
				bvos[i].setBbarcodeclose(UFBoolean.FALSE);
				bvos[i].setBonroadflag(UFBoolean.FALSE);
				bvos[i].setCastunitid(castunitid);
				bvos[i].setCbodytranstypecode("4I-01");
				bvos[i].setCbodywarehouseid(cwarehouseid);
//				bvos[i].setClocationid(Locationid);
				bvos[i].setCmaterialoid(cmaterialoid);
				bvos[i].setCmaterialvid(pk_source);
				bvos[i].setCorpoid(pk_org);
				bvos[i].setCorpvid(pk_org_v);
				bvos[i].setCrowno(String.valueOf(i)+"0");
				bvos[i].setCunitid(zdw);
//				bvos[i].setDinbounddate(dmakedate_d);
				bvos[i].setIbcversion(1);
				bvos[i].setNassistnum(nastnum);
				bvos[i].setNnum(nnum);
				bvos[i].setNshouldassistnum(nastnum);
				bvos[i].setNshouldnum(nnum);
				bvos[i].setCstateid(cstateid);
				bvos[i].setClocationid(clocationid);
				bvos[i].setNvolume(UFDouble.ZERO_DBL);
				if(!unitweight.equals(UFDouble.ZERO_DBL))
					bvos[i].setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
				bvos[i].setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
				bvos[i].setPk_group(pk_group);
				bvos[i].setPk_org(pk_org);
				bvos[i].setPk_org_v(pk_org_v);
				bvos[i].setPseudoColumn(0);
				bvos[i].setVbatchcode(vbatchcode);
				bvos[i].setDproducedate(new UFDate(dmakedate));
				bvos[i].setDvalidate(getDvalidate(cmaterialoid,pk_org,new UFDate(dmakedate)));
//				bvos[i].setCprojectid(cprojectid);
				bvos[i].setVchangerate(measrate);
				bvos[i].setDbizdate(dmakedate_d);
				bvos[i].setStatus(VOStatus.NEW);
			}
			GOVO.setParentVO(hvo);
			GOVO.setChildrenVO(bvos);
			result=(GeneralOutVO[])pfaction.processAction("WRITE", "4I", null, GOVO,null, null);
			successMessage = "NC其它出库单保存";
			if(sighFlag.equals("Y")){
				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
				pfaction.processAction("SIGN", "4I", null, result[0], null, null);
				successMessage = successMessage+"并签字";
			}
			successMessage = successMessage+"成功！";
			returnJson.setStatus("1");
			returnJson.setResultBillcode(GOVO.getParentVO().getAttributeValue("vbillcode").toString());
			returnJson.setReturnMessage(successMessage);
		} catch (Exception e) {
			returnJson.setStatus("0");
			returnJson.setResultBillcode("");
			returnJson.setReturnMessage(e.toString());
			if(result!=null&&result.length>0){
				String pk = result[0].getParentVO().getCgeneralhid();
				GeneralOutVO errorVo = IQ.querySingleBillByPk(GeneralOutVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4I", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(returnJson);
	}
	
    public JSONString Generate4A(JSONObject jsonAy) {
		// TODO 自动生成的方法存根
		returnvo returnJson = new returnvo();
		GeneralInVO [] result = null;
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		try {
			GeneralInVO GIVO = new GeneralInVO();
			GeneralInHeadVO hvo = new GeneralInHeadVO();
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //入库仓库id
//			String userCode = jsonAy.getString("userCode");  //用户编码
			String vnote = jsonAy.getString("vnote");  //表头备注
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			String cwhsmanagerid = jsonAy.getString("cwhsmanagerid");  //库管员id
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			UFDate dmakedate_d = dmakedate_t.getDate();
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
//			String sql_userid = "select u.cuserid from sm_user u where u.user_code='"+userCode+"'";
			String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		    String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
			String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
//			String cuserid = (String) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql_userid, new ColumnProcessor());
			JSONArray list = jsonAy.getJSONArray("list");
			GeneralInBodyVO[] bvos = new GeneralInBodyVO[list.size()];
			String successMessage = "";
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
			String pk_dept = jsonAy.getString("pk_dept");  //部门ID
			String pk_dept_v = queryOne("select v.pk_vid from org_dept_v v where v.pk_dept='"+pk_dept+"'");
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
			String ctrantypeid = jsonAy.getString("ctrantypeid");  //出入库类型ID
			if(ctrantypeid==null||ctrantypeid.equals(""))
				ctrantypeid = "0001A110000000002DXV";
			String vtrantypecode = queryOne("select t.pk_billtypecode from bd_billtype t where t.pk_billtypeid='"+ctrantypeid+"'");
			
			hvo.setCorpoid(pk_org);
			hvo.setCorpvid(pk_org_v);
			hvo.setCtrantypeid(ctrantypeid);
			hvo.setCwarehouseid(cwarehouseid);
			hvo.setCwhsmanagerid(cwhsmanagerid);
			hvo.setFbillflag(2);
			hvo.setIprintcount(0);
			hvo.setPk_group(pk_group);
			hvo.setPk_org(pk_org);
			hvo.setPk_org_v(pk_org_v);
			hvo.setVnote(vnote);
			hvo.setVtrantypecode(vtrantypecode);
			hvo.setCdptid(pk_dept);
			hvo.setCdptvid(pk_dept_v);
			hvo.setStatus(VOStatus.NEW);
			for(int i=0;i<list.size();i++){
				String cmaterialoid = list.getJSONObject(i).getString("cmaterialoid");  //物料id
				String clocationCode = list.getJSONObject(i).getString("clocationCode");  //货位编码
				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));   //主数量
				UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));	//辅数量			
				String vbatchcode = list.getJSONObject(i).getString("vbatchcode");  //批次号
				String cprojectid = list.getJSONObject(i).getString("cprojectid")==null?"":list.getJSONObject(i).getString("cprojectid");  //项目id
				String prodate = list.getJSONObject(i).getString("prodate")==null?(new UFDate(System.currentTimeMillis())).toString():list.getJSONObject(i).getString("prodate");  //生产日期
				String cstateid = list.getJSONObject(i).getString("cstateid");
				UFDate prodate_d = new UFDate(prodate);
				String Locationid = GetLocationid(cwarehouseid,clocationCode);
				String clocationid = list.getJSONObject(i).getString("clocationid") == null? "" : list.getJSONObject(i).getString("clocationid");//货位
				
				String sql_mv = "select v.pk_source from bd_material_v v where v.pk_material='"+cmaterialoid+"'";
				
				String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
						+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
				HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
				String castunitid = "";
				String pk_source = "";
				String zdw = "";
				String measrate = "";
				if(materialMap!=null&&materialMap.get("zdw")!=null){
					castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
					pk_source=materialMap.get("pk_source").toString();
					zdw=materialMap.get("zdw").toString();
					measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
				}else
					throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
				
				HashMap<String,Object> matinfo = queryMaterialBD(cmaterialoid);//查询物料单位重量
				UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight")));
				bvos[i] = new GeneralInBodyVO();
				bvos[i].setBbarcodeclose(UFBoolean.FALSE);
				bvos[i].setBcseal(UFBoolean.FALSE);
				bvos[i].setBonroadflag(UFBoolean.FALSE);
				bvos[i].setCastunitid(castunitid);
				bvos[i].setCbodytranstypecode("4A-01");
				bvos[i].setCbodywarehouseid(cwarehouseid);
				bvos[i].setClocationid(Locationid);
				bvos[i].setCmaterialoid(cmaterialoid);
				bvos[i].setCmaterialvid(pk_source);
				bvos[i].setCorpoid(pk_org);
				bvos[i].setCorpvid(pk_org_v);
				bvos[i].setCrowno(String.valueOf(i)+"0");
				bvos[i].setCunitid(zdw);
				bvos[i].setDinbounddate(dmakedate_d);
				bvos[i].setDproducedate(prodate_d);
				bvos[i].setCprojectid(cprojectid);
				bvos[i].setIbcversion(1);
				bvos[i].setNassistnum(nastnum);
				bvos[i].setNnum(nnum);
				bvos[i].setNshouldassistnum(nastnum);
				bvos[i].setNshouldnum(nnum);
				bvos[i].setNvolume(UFDouble.ZERO_DBL);
				if(!unitweight.equals(UFDouble.ZERO_DBL))
					bvos[i].setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
				bvos[i].setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
				bvos[i].setPk_group(pk_group);
				bvos[i].setPk_org(pk_org);
				bvos[i].setPk_org_v(pk_org_v);
				bvos[i].setPseudoColumn(0);
				bvos[i].setVbatchcode(vbatchcode);
				bvos[i].setDvalidate(getDvalidate(cmaterialoid,pk_org,dmakedate_d));
				bvos[i].setDbizdate(dmakedate_d);
				bvos[i].setTbcts(dmakedate_t);
				bvos[i].setVchangerate(measrate);
				bvos[i].setCstateid(cstateid);
				bvos[i].setClocationid(clocationid);
				bvos[i].setStatus(VOStatus.NEW);
			}
			GIVO.setParentVO(hvo);
			GIVO.setChildrenVO(bvos);
			result=(GeneralInVO[])pfaction.processAction("WRITE", "4A", null, GIVO,null, null);
			successMessage = "NC其它入库单保存";
			if(sighFlag.equals("Y")){
				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
				pfaction.processAction("SIGN", "4A", null, result[0], null, null);
				successMessage = successMessage+"并签字";
			}
			successMessage = successMessage+"成功！";
			returnJson.setStatus("1");
			returnJson.setResultBillcode(GIVO.getParentVO().getAttributeValue("vbillcode").toString());
			returnJson.setReturnMessage(successMessage);
		} catch (Exception e) {
			returnJson.setStatus("0");
			returnJson.setResultBillcode("");
			returnJson.setReturnMessage(e.toString());
			if(result!=null&&result.length>0){
				String pk = result[0].getParentVO().getCgeneralhid();
				GeneralInVO errorVo = IQ.querySingleBillByPk(GeneralInVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4A", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(returnJson);
    	
//		returnvo returnJson = new returnvo();
//		GeneralOutVO[] result = null;
//		String successMessage = "";
//		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
//		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
//		try {
//			String userCode = jsonAy.getString("userCode");  //用户编码
//			String sql_userid = "select u.cuserid from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where p.code='"+userCode+"'";
//
//			String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
//			if(cuserid==null)
//				throw new Exception("制单人编码"+userCode+"在NC不存在，请检查！");
//			String approver = jsonAy.getString("approver");  //审批人编码
//			String sql_approver = "select u.cuserid from sm_user u inner join bd_psndoc p on u.pk_psndoc=p.pk_psndoc where p.code='"+approver+"'";
//			String approverid = (String) dao.executeQuery(sql_approver, new ColumnProcessor());
//			if(approverid==null)
//				throw new Exception("审批人编码"+approver+"在NC不存在，请检查！");
//
//			String vnote = jsonAy.getString("vnote");  //备注
//			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
////			String replenishflag = jsonAy.getString("replenishflag");  //是否退库
////			UFBoolean replenishflag2 = UFBoolean.FALSE;
////			if(replenishflag!=null&&replenishflag.equals("Y"))
////				replenishflag2 = UFBoolean.TRUE;
//			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
//			UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
//			UFDate dmakedate_d = new UFDate(dmakedate);	
//			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
//			String cspecialhid = jsonAy.getString("cspecialhid");  //转库单表头ID
//			String deptid = jsonAy.getString("deptid");  //部门ID
//			String deptvid = queryOne("select v.pk_vid from org_dept_v v where v.pk_dept='"+deptid+"'");
//			JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
//			String sql_group = "select h.pk_group from ic_whstrans_h h where h.cspecialhid='"+cspecialhid+"'";
//			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
//			
////			String wms_id = jsonAy.getString("wms_id");  //WMS系统单据主键
//			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
//			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
//			
//			WhsTransBillVO AP = IQ.querySingleBillByPk(WhsTransBillVO.class, cspecialhid);
//			String ResultBillcode = "";
//			if (AP==null){
//				throw new Exception("传输的转库单ID在NC系统中不存在！");
//			}
//			
//			GeneralInVO PV = (GeneralInVO)PfUtilTools.runChangeData("4K", "4A", AP);  
//
//			PV.getParentVO().setCdptid(deptid);
//			PV.getParentVO().setCdptvid(deptvid);
//			PV.getParentVO().setVnote(vnote);
//			PV.getParentVO().setStatus(VOStatus.NEW);
//			
//			GeneralInBodyVO[] bodys = (GeneralInBodyVO[])PV.getChildrenVO();
//			ArrayList<GeneralInBodyVO> oi_new = new ArrayList<GeneralInBodyVO>();
////			MaterialOutBodyVO[] MBV = new MaterialOutBodyVO[bodys.length];
//			for (int s=0;s<list.size();s++){
//				String cspecialbid = list.getJSONObject(s).getString("cspecialbid")==null?"null":list.getJSONObject(s).getString("cspecialbid");
//				UFDouble nnum = new UFDouble(list.getJSONObject(s).getString("nnum"));   //主数量
//				UFDouble nastnum = new UFDouble(list.getJSONObject(s).getString("nastnum"));	//辅数量
//				for (int t=0;t<bodys.length;t++){
//					String sourcebillbid = bodys[t].getCsourcebillbid();
//					if(sourcebillbid.equals(cspecialbid)) {
//						GeneralInBodyVO newbody = (GeneralInBodyVO)bodys[t].clone();
//						newbody.setNnum(nnum);
//						newbody.setNassistnum(nastnum);
//						newbody.setStatus(VOStatus.NEW);
//						oi_new.add(newbody);
//					}
//					
//				}
//			}
//			PV.setChildrenVO(oi_new.toArray(new GeneralInBodyVO[oi_new.size()]));
//			
//			result = (GeneralOutVO[])pfaction.processAction("WRITE", "4A", null, PV, null, null);
//			ResultBillcode = ResultBillcode+"【"+result[0].getParentVO().getVbillcode()+"】";
//			successMessage = "NC其他入库单保存";
//			if(sighFlag.equals("Y")){
//				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
//				String taudittime = jsonAy.getString("taudittime");  //签字日期
//				UFDateTime taudittime_t = new UFDateTime(taudittime);			
//				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
//				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
//				pfaction.processAction("SIGN", "4A", null, result[0], null, null);
//				successMessage = successMessage+"并签字";
//			}
//			successMessage = successMessage+"成功！";
//			
//			returnJson.setResultBillcode(ResultBillcode);
//			returnJson.setReturnMessage(successMessage);
//			returnJson.setStatus("1");
//		} catch (Exception e) {
//			// TODO 自动生成的 catch 块
//			returnJson.setResultBillcode("");
//			returnJson.setStatus("0");
//			returnJson.setReturnMessage(e.toString());
//		}
//		return RestUtils.toJSONString(returnJson);
	}
	
	private HashMap queryDvalidate(String code,String materialPK) throws Exception{
		String sql ="select s.dvalidate,s.dproducedate from scm_batchcode s where s.vbatchcode='"+code+"' and s.cmaterialoid='"+materialPK+"' and s.dr=0";
		HashMap result = (HashMap) dao.executeQuery(sql,new MapProcessor());
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
	
	private String InfoQuery(String code,String type) throws Exception{
		  String sql = null;
		  HashMap id = null;
		  switch(type){
		  case "arriveorderCode":
			  sql = "select vbillcode from po_arriveorder where pk_arriveorder='"+code+"' and dr=0";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("vbillcode")!=null)
					return id.get("vbillcode").toString();
				else
					return ""; 
		  case "cwfl":
			  sql = "select def1 from bd_marbasclass where pk_marbasclass=(select pk_marbasclass from bd_material where pk_material='"+code+"')";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("def1")!=null)
					return id.get("def1").toString();
				else
					return "";
		  case "flbm":
			  sql = "select substr(c.code,0,2) code from bd_material m inner join bd_marbasclass c on m.pk_marbasclass=c.pk_marbasclass where m.pk_material='"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("code")!=null)
					return id.get("code").toString();
				else
					return "";
		  case "pk_plandept_v":
			  sql = "select max(v.pk_vid) pk_vid from org_dept_v v where v.pk_dept='"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("pk_vid")!=null)
					return id.get("pk_vid").toString();
				else
					return "";
		  case "sfww":
			  sql = "select s.martype from bd_materialstock s where s.pk_material||s.pk_org='"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("martype")!=null)
					return id.get("martype").toString();
				else
					return "";
		  case "shdz":
			  sql = "select address from bd_custaddress_v_a where pk_address = '"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("address")!=null)
					return id.get("address").toString();
				else
					return "";
		  case "cfirstbillhid":
			  sql = "select ph.pk_order from po_order ph inner join po_order_b pb on ph.pk_order=pb.pk_order where pb.pk_order_b='"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("pk_order")!=null)
					return id.get("pk_order").toString();
				else
					return "";
		  case "cgddh":
			  sql = "select ph.vbillcode from po_order ph inner join po_order_b pb on ph.pk_order=pb.pk_order where pb.pk_order_b='"+code+"'";
			  id = (HashMap) dao.executeQuery(sql,new MapProcessor());
			  if (id!=null&&id.get("vbillcode")!=null)
					return id.get("vbillcode").toString();
				else
					return "";
		  } 
		  return null;
	  }
	
	private String GetLocationid(String Cwarehouseid,String clocationCode) throws Exception{
		String sql_location = "select r.pk_rack from bd_rack r where r.pk_stordoc='"+Cwarehouseid+"' and r.code='"+clocationCode+"'";
		String clocationid = (String) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql_location, new ColumnProcessor());
		if(clocationid==null){
			if(clocationCode!=null&&!clocationCode.equals(""))
				throw new Exception("货位"+clocationCode+"不属于入库单的入库仓库，请检查货位！");
			else
				return "";
		}
		else
			return clocationid;
	}
	
	private HashMap<String,Object> queryMaterialBD(String pk_material) throws DAOException{
		String sql = "select unitweight from bd_material m where m.pk_material='"+pk_material+"'";
		HashMap<String,Object> result = (HashMap) dao.executeQuery(sql,new MapProcessor());  
		return result;
	}
	
	private HashMap<String,Object> queryPo(String pk_order_b) throws DAOException{
		String sql = "select nvl(b.nnum,0)-nvl(b.naccumstorenum,0) krkzsl,(nvl(b.nnum,0)-nvl(b.naccumstorenum,0))*(b.nastnum/b.nnum) krksl "
				+ "from po_order_b b where b.dr=0 and b.pk_order_b='"+pk_order_b+"'";
		HashMap<String,Object> result = (HashMap) dao.executeQuery(sql,new MapProcessor());  
		return result;
	}
	
	private HashMap<String,Object> queryArr(String pk_arriveorder_b) throws DAOException{
		String sql = "select nvl(b.nnum,0)-nvl(b.naccumstorenum,0) krkzsl,(nvl(b.nnum,0)-nvl(b.naccumstorenum,0))*(b.nastnum/b.nnum) krksl from po_arriveorder_b b where b.pk_arriveorder_b='"+pk_arriveorder_b+"' and h.dr=0 and b.dr=0";
		HashMap<String,Object> result = (HashMap) dao.executeQuery(sql,new MapProcessor());  
		return result;
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
	
	/**
	 * 查询一个字符
	 * zhoush
	 * 2023-5-6
	 */
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
	
	private String getusingstatus(String code) {
		return queryOne("select s.pk_usingstatus from fa_usingstatus s where s.status_code='"+code+"'");
	}
	
	 public JSONString Generate46(JSONObject jsonAy) {
			returnvo returnJson = new returnvo();                
			String successMessage = "";
			FinProdInVO[] FP_origin = null;
			AggWrVO[] WV = null;
			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			IPwrMaintainService IPM = (IPwrMaintainService) NCLocator.getInstance().lookup(IPwrMaintainService.class);
			IPMOBusinessService PmoService = (IPMOBusinessService) NCLocator.getInstance().lookup(IPMOBusinessService.class);
			IWrBusinessService WrService = (IWrBusinessService) NCLocator.getInstance().lookup(IWrBusinessService.class);
			try {
				String wmsID = jsonAy.getString("wmsID");  
				if(wmsID!=null) {
					String sql_wmsid = "select h.vbillcode from ic_finprodin_h h where h.vdef1='"+wmsID+"' and h.dr=0";
					String wmsflag = (String) dao.executeQuery(sql_wmsid, new ColumnProcessor());
					if(wmsflag!=null) {
						returnJson.setResultBillcode(wmsflag);
						returnJson.setReturnMessage("NC产成品入库单保存并签字成功！");
						returnJson.setStatus("1");
						return RestUtils.toJSONString(returnJson);
//						throw new Exception("ID为"+wmsID+"的WMS单据已在NC生成过产成品入库单，重复推单，请检查！");
					}
						
				}
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

				String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
//				String replenishflag = jsonAy.getString("replenishflag");  //是否退库
//				UFBoolean replenishflag2 = UFBoolean.FALSE;
//				if(replenishflag!=null&&replenishflag.equals("Y"))
//					replenishflag2 = UFBoolean.TRUE;
				String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
				UFDate dmakedate_d = dmakedate_t.getDate();	
				InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
				String cpmohid = jsonAy.getString("cpmohid");  //生产订单ID
				String scddlx = queryOne("select h.ctrantypeid from mm_pmo h where h.cpmohid='"+cpmohid+"'");  //生产订单类型
				String cmoid = jsonAy.getString("cmoid");  //生产订单表体ID
				String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
				String sql_group = "select p.pk_group from mm_pmo p where p.cpmohid='"+cpmohid+"'";
				String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
				InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
				InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
						
				UFDouble nbwrastnum = jsonAy.getString("nbwrastnum")==null?UFDouble.ZERO_DBL:new UFDouble(jsonAy.getString("nbwrastnum"));  //完工数量
				UFDouble nbwrnum = jsonAy.getString("nbwrnum")==null?UFDouble.ZERO_DBL:new UFDouble(jsonAy.getString("nbwrnum"));  //完工主数量
				String batchcode = jsonAy.getString("batchcode");  //入库批次号
				String vbatchcodenote = jsonAy.getString("vbatchcodenote");  //批次备注
				String cstateid = jsonAy.getString("cstateid");
				String[] pks = {cpmohid};
				AbstractBill[] PV=(AbstractBill[])IQ.queryAbstractBillsByPks(PMOAggVO.class, pks);
				PMOAggVO pmovo = (PMOAggVO)PV[0];
				PMOAggVO[] pmos = {pmovo};
				
				if (PV==null||PV.length==0){
					throw new Exception("传输的生产订单ID在系统不存在或者已删除!");
				}
				PMOItemVO[] piv = (PMOItemVO[])PV[0].getChildrenVO();
				List<PMOItemVO> bodylist = new ArrayList();
				String fitemstatus = String.valueOf(piv[0].getAttributeValue("fitemstatus"));
				if(fitemstatus.equals("4")) {
					PmoService.put(pmos);
				}
				for(int w=0;w<piv.length;w++) {
					String bid = piv[w].getCmoid();
					if(bid.equals(cmoid)) {
//						if(fitemstatus.equals("2")){
//							throw new Exception("传输的生产订单已完工，不能重复生成完工报告！");
//						}else if(fitemstatus.equals("3")){
//							throw new Exception("传输的生产订已关闭！");
//						}else if(!fitemstatus.equals("1")){
//							throw new Exception("传输的生产订状态为未投放！");
//						}
						bodylist.add(piv[w]);
						break;
					}
				}
				PV[0].setChildrenVO(bodylist.toArray(new PMOItemVO[bodylist.size()]));
				WV = (AggWrVO[])PfUtilTools.runChangeDataAry("55A2","55A4", PV);
				WV[0].getParentVO().setFbillstatus(1);
				WrItemVO[] WI = WV[0].getChildrenVO();
				WI[0].setVbrowno("10");
				WI[0].setNbwrastnum(nbwrastnum);
				WI[0].setNbwrnum(nbwrnum);
				WI[0].setNbcheckastnum(nbwrastnum);
				WI[0].setNbchecknum(nbwrnum);
				WI[0].setVbinbatchcode(batchcode);
				WI[0].setFbproducttype(1);
				WI[0].setTbstarttime(new UFDateTime(System.currentTimeMillis()));
				WI[0].setTbendtime(new UFDateTime(System.currentTimeMillis()+1));
				WV = IPM.insert(WV);
				AggWrVO[] WV2 = (AggWrVO[])pfaction.processAction("APPROVE", "55A4", null, WV[0], null, null);
//				FinProdInVO[] pv = (FinProdInVO[])pfaction.processAction("55A4", "46", null, WV2[0], null, null);
				FinProdInVO pv = (FinProdInVO)PfUtilTools.runChangeData("55A4", "46", WV2[0]);
				pv.getHead().setStatus(VOStatus.NEW);
				pv.getHead().setCwarehouseid(cwarehouseid);
				pv.getHead().setCprowarehouseid(cwarehouseid);
				pv.getHead().setVdef1(wmsID);
				FinProdInBodyVO pbv = pv.getBody(0);
				if(scddlx!=null&&scddlx.equals("0001A110000000002DZI")) {  //当生产订单类型为返工流程生产订单时，产成品入库也设为该类型
					pv.getHead().setVtrantypecode("46-Cxx-10");
					pv.getHead().setCtrantypeid("1001A2100000002LVM5K");
					pbv.setBreworkflag(UFBoolean.TRUE);
				}else {
					pv.getHead().setVtrantypecode("46-01");
					pv.getHead().setCtrantypeid("0001A110000000002DXT");	
				}
				pbv.setNassistnum(pbv.getNshouldassistnum());
				pbv.setNnum(pbv.getNshouldnum());
				pbv.setVbatchcode(batchcode);
				pbv.setPk_batchcode(queryPK_batchcode(batchcode,pbv.getCmaterialoid()));
				pbv.setVbatchcodenote(vbatchcodenote);
				pbv.setCstateid(cstateid);
//				pbv.setCstateid("1001A210000000070OZ8");  //库存状态
				pbv.setCbodywarehouseid(cwarehouseid);
				pbv.setDproducedate(dmakedate_d);
				pbv.setDbizdate(dmakedate_d);
				pbv.setDvalidate(getDvalidate(pbv.getCmaterialoid(),pv.getHead().getPk_org(),dmakedate_d));
				pbv.setStatus(VOStatus.NEW);
				FP_origin = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, pv, null, null); 
				dao.executeUpdate("update mm_mo m set m.ninnum=(select sum(fb.nnum) from ic_finprodin_b fb where fb.fproductclass=1 and fb.cfirstbillbid='"+cmoid+"' "
						+ "and fb.dr=0),m.ninastnum=(select sum(fb.nassistnum) from ic_finprodin_b fb where fb.fproductclass=1 and fb.cfirstbillbid='"+cmoid+"' and fb.dr=0) where m.cmoid='"+cmoid+"'");
				successMessage = "NC产成品入库单保存";
				if(sighFlag.equals("Y")) {
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "46", null, FP_origin[0],null, null);
					successMessage = successMessage+"并签字";
				}
				successMessage = successMessage+"成功！";
				rewriteWr(WV[0].getParentVO().getPk_wr());
				returnJson.setResultBillcode(FP_origin[0].getParentVO().getVbillcode());
				returnJson.setReturnMessage(successMessage);
				returnJson.setStatus("1");
			} catch (Exception e) {
				// TODO 自动生成的 catch 块
				returnJson.setResultBillcode("");
				returnJson.setStatus("0");
				returnJson.setReturnMessage(e.toString());
				if(FP_origin!=null&&FP_origin.length>0){
					String pk = FP_origin[0].getParentVO().getCgeneralhid();
					FinProdInVO errorVo = IQ.querySingleBillByPk(FinProdInVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "46", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
				if(WV!=null&&WV.length>0){
					String pk = WV[0].getParentVO().getPk_wr();
					AggWrVO errorVo = IQ.querySingleBillByPk(AggWrVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("UNAPPROVE", "55A4", null, errorVo, null, null);
						pfaction.processAction("DELETE", "55A4", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(returnJson);
	 }
	 
	 public JSONString Generate46v2(JSONObject jsonAy) {
			returnvo returnJson = new returnvo();                
			String successMessage = "";
			FinProdInVO[] FP_origin = null;
			AggWrVO[] WV = null;
			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			IPwrMaintainService IPM = (IPwrMaintainService) NCLocator.getInstance().lookup(IPwrMaintainService.class);
			IPMOBusinessService PmoService = (IPMOBusinessService) NCLocator.getInstance().lookup(IPMOBusinessService.class);
			IWrBusinessService WrService = (IWrBusinessService) NCLocator.getInstance().lookup(IWrBusinessService.class);
			try {
				String wmsID = jsonAy.getString("wmsID");  
				if(wmsID!=null) {
					String sql_wmsid = "select h.vbillcode from ic_finprodin_h h where h.vdef1='"+wmsID+"' and h.dr=0";
					String wmsflag = (String) dao.executeQuery(sql_wmsid, new ColumnProcessor());
					if(wmsflag!=null) {
						returnJson.setResultBillcode(wmsflag);
						returnJson.setReturnMessage("NC产成品入库单保存并签字成功！");
						returnJson.setStatus("1");
						return RestUtils.toJSONString(returnJson);
//						throw new Exception("ID为"+wmsID+"的WMS单据已在NC生成过产成品入库单，重复推单，请检查！");
					}
						
				}
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

				String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
				String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
				UFDate dmakedate_d = dmakedate_t.getDate();	
				InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());

//				String cmoid = jsonAy.getString("cmoid");  //生产订单表体ID
				String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
                                if(cwarehouseid==null)
					throw new Exception("仓库ID不能为空！");
				String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
				String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
                                if(pk_group==null)
					throw new Exception("仓库ID在NC不存在，请检查！");
				InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
				InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
						
				JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
				ArrayList<String> pks_list = new ArrayList<String>();
				Map pks_map = new HashMap();
				String scddlx="";
				for(int i=0;i<list.size();i++) {  
					String pk = list.getJSONObject(i).getString("cpmohid")==null?"":list.getJSONObject(i).getString("cpmohid");
					String scddid = queryOne("select * from mm_pmo p where p.cpmohid='"+pk+"' and p.dr=0");
					if(scddid==null)
						throw new Exception("生产订单ID："+pk+"在NC不存在或者已删除!");
					if(pks_map.get(pk)==null) {
						pks_list.add(pk);
						pks_map.put(pk, pk);
					}
					scddlx = queryOne("select h.ctrantypeid from mm_pmo h where h.cpmohid='"+pk+"'");  //生产订单类型
				}
				String[] pks = pks_list.toArray(new String[pks_list.size()]);
				AbstractBill[] PV=(AbstractBill[])IQ.queryAbstractBillsByPks(PMOAggVO.class, pks);
				for(int j=0;j<PV.length;j++) {
					PMOAggVO pmovo = (PMOAggVO)PV[j];
					PMOAggVO[] pmos = {pmovo};

					PMOItemVO[] piv = (PMOItemVO[])PV[j].getChildrenVO();
					String fitemstatus = String.valueOf(piv[0].getAttributeValue("fitemstatus"));
					if(fitemstatus.equals("4")) {
						PmoService.put(pmos);
					}	
				}
				
				WV = (AggWrVO[])PfUtilTools.runChangeDataAry("55A2","55A4", PV);
				List<WrItemVO> wgblist = new ArrayList();
				AggWrVO wghvo = new AggWrVO();
				WV[0].getParentVO().setFbillstatus(1);
				wghvo.setParentVO(WV[0].getParentVO());
				
				for(int r=0;r<WV.length;r++) {
					WrItemVO[] WI = WV[0].getChildrenVO();
					for(int s=0;s<WI.length;s++) {
						String Cbfirstmobid = WI[s].getCbfirstmobid();
						for (int t=0;t<list.size();t++){
							String cmoid = list.getJSONObject(t).getString("cmoid")==null?"null":list.getJSONObject(t).getString("cmoid");
							UFDouble nbwrnum = list.getJSONObject(t).getString("nbwrnum")==null?null:new UFDouble(list.getJSONObject(t).getString("nbwrnum"));
							UFDouble nbwrastnum = list.getJSONObject(t).getString("nbwrastnum")==null?null:new UFDouble(list.getJSONObject(t).getString("nbwrastnum"));
							String batchcode = list.getJSONObject(t).getString("batchcode")==null?"null":list.getJSONObject(t).getString("batchcode");
							if(cmoid.equals(Cbfirstmobid)) {
								
								WrItemVO newitem = (WrItemVO)WI[s].clone();
								newitem.setVbrowno(String.valueOf(t)+"0");
								newitem.setNbwrastnum(nbwrastnum);
								newitem.setNbwrnum(nbwrnum);
								newitem.setNbcheckastnum(nbwrastnum);
								newitem.setNbchecknum(nbwrnum);
								newitem.setVbinbatchcode(batchcode);
								newitem.setFbproducttype(1);
								newitem.setTbstarttime(new UFDateTime(System.currentTimeMillis()));
								newitem.setTbendtime(new UFDateTime(System.currentTimeMillis()+1));
								wgblist.add(newitem);
							}
						}
					}
				}
				wghvo.setChildrenVO(wgblist.toArray(new WrItemVO[wgblist.size()]));
				AggWrVO[] wgaggvo = {wghvo};
				WV = IPM.insert(wgaggvo);
				AggWrVO[] WV2 = (AggWrVO[])pfaction.processAction("APPROVE", "55A4", null, WV[0], null, null);
//				FinProdInVO[] pv = (FinProdInVO[])pfaction.processAction("55A4", "46", null, WV2[0], null, null);
				FinProdInVO pv = (FinProdInVO)PfUtilTools.runChangeData("55A4", "46", WV2[0]);
				pv.getHead().setStatus(VOStatus.NEW);
				pv.getHead().setCwarehouseid(cwarehouseid);
				pv.getHead().setCprowarehouseid(cwarehouseid);
				pv.getHead().setVdef1(wmsID);
//				FinProdInBodyVO pbv = pv.getBody(0);
				UFBoolean Breworkflag = UFBoolean.FALSE;
				if(scddlx!=null&&scddlx.equals("0001A110000000002DZI")) {  //当生产订单类型为返工流程生产订单时，产成品入库也设为该类型
					pv.getHead().setVtrantypecode("46-Cxx-10");
					pv.getHead().setCtrantypeid("1001A2100000002LVM5K");
					Breworkflag = UFBoolean.TRUE;
//					pbv.setBreworkflag(UFBoolean.TRUE);
				}else {
					pv.getHead().setVtrantypecode("46-01");
					pv.getHead().setCtrantypeid("0001A110000000002DXT");	
				}
				FinProdInBodyVO[] pbvs = pv.getBodys();
				List<String> sqllist = new ArrayList();
				Map sendmap = new HashMap();
				for(int q=0;q<pbvs.length;q++) {
					String cfirstbillbid = pbvs[q].getCfirstbillbid();
					for(int c=0;c<list.size();c++) {
						String cmoid = list.getJSONObject(c).getString("cmoid")==null?"null":list.getJSONObject(c).getString("cmoid");
						UFDouble nbwrnum = list.getJSONObject(c).getString("nbwrnum")==null?null:new UFDouble(list.getJSONObject(c).getString("nbwrnum"));
						UFDouble nbwrastnum = list.getJSONObject(c).getString("nbwrastnum")==null?null:new UFDouble(list.getJSONObject(c).getString("nbwrastnum"));
						String batchcode = list.getJSONObject(c).getString("batchcode")==null?"null":list.getJSONObject(c).getString("batchcode");
						String vbatchcodenote = list.getJSONObject(c).getString("vbatchcodenote")==null?"null":list.getJSONObject(c).getString("vbatchcodenote");
						String cstateid = list.getJSONObject(c).getString("cstateid")==null?"null":list.getJSONObject(c).getString("cstateid");
						if(cfirstbillbid.equals(cmoid)) {
							if(sendmap.get(c)!=null)
								continue;
							pbvs[q].setNassistnum(pbvs[q].getNshouldassistnum());
							pbvs[q].setNnum(pbvs[q].getNshouldnum());
							pbvs[q].setVbatchcode(batchcode);
							pbvs[q].setPk_batchcode(queryPK_batchcode(batchcode,pbvs[q].getCmaterialoid()));
							pbvs[q].setVbatchcodenote(vbatchcodenote);
							pbvs[q].setCstateid(cstateid);
							pbvs[q].setBreworkflag(Breworkflag);
//							pbv.setCstateid("1001A210000000070OZ8");  //库存状态
							pbvs[q].setCbodywarehouseid(cwarehouseid);
							pbvs[q].setDproducedate(dmakedate_d);
							pbvs[q].setDbizdate(dmakedate_d);
							pbvs[q].setDvalidate(getDvalidate(pbvs[q].getCmaterialoid(),pv.getHead().getPk_org(),dmakedate_d));
							pbvs[q].setStatus(VOStatus.NEW);
							sqllist.add("update mm_mo m set m.ninnum=(select sum(fb.nnum) from ic_finprodin_b fb where fb.fproductclass=1 and fb.cfirstbillbid='"+cmoid+"' "
						+ "and fb.dr=0),m.ninastnum=(select sum(fb.nassistnum) from ic_finprodin_b fb where fb.fproductclass=1 and fb.cfirstbillbid='"+cmoid+"' and fb.dr=0) where m.cmoid='"+cmoid+"'");
							sendmap.put(c, c);
							break;
						}
						
					}
				}
				FP_origin = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, pv, null, null); 
				for(int g=0;g<sqllist.size();g++)
					dao.executeUpdate(sqllist.get(g));
				successMessage = "NC产成品入库单保存";
				if(sighFlag.equals("Y")) {
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "46", null, FP_origin[0],null, null);
					successMessage = successMessage+"并签字";
				}
				successMessage = successMessage+"成功！";
				rewriteWr(WV[0].getParentVO().getPk_wr());
				returnJson.setResultBillcode(FP_origin[0].getParentVO().getVbillcode());
				returnJson.setReturnMessage(successMessage);
				returnJson.setStatus("1");
			} catch (Exception e) {
				// TODO 自动生成的 catch 块
				returnJson.setResultBillcode("");
				returnJson.setStatus("0");
				returnJson.setReturnMessage(e.toString());
				if(FP_origin!=null&&FP_origin.length>0){
					String pk = FP_origin[0].getParentVO().getCgeneralhid();
					FinProdInVO errorVo = IQ.querySingleBillByPk(FinProdInVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "46", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
				if(WV!=null&&WV.length>0){
					String pk = WV[0].getParentVO().getPk_wr();
					AggWrVO errorVo = IQ.querySingleBillByPk(AggWrVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("UNAPPROVE", "55A4", null, errorVo, null, null);
						pfaction.processAction("DELETE", "55A4", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(returnJson);
	 }
	 
	 //回写完工入库的入库主数量
	 private void rewriteWr(String pk_wr) throws Exception{
			String sql ="select q.pk_wr_quality from mm_wr_product p inner join mm_wr_quality q on p.pk_wr_product=q.pk_wr_product_q where p.pk_wr='"+pk_wr+"' and p.dr=0 and q.dr=0";
			List<Object[]> results = (List<Object[]>) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql, new ArrayListProcessor());
			if (results != null && results.size() > 0){
				for(int k=0;k<results.size();k++){
					String pk_wr_quality = String.valueOf(results.get(k)[0]);	
					String sql2 = "update mm_wr_quality q set q.nginnum=q.ngnum, q.nginastnum=q.ngastnum where q.pk_wr_quality='"+pk_wr_quality+"'";
					dao.executeUpdate(sql2);
				}
			}
		}
	 
	 public JSONString Generate46_NS(JSONObject jsonAy) {
			// TODO 自动生成的方法存根
			returnvo returnJson = new returnvo();
			String successMessage = "";
			FinProdInVO[] result = null;
			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
				try {
					String wmsID = jsonAy.getString("wmsID");  
					if(wmsID!=null) {
						String sql_wmsid = "select h.vbillcode from ic_finprodin_h h where h.vdef1='"+wmsID+"' and h.dr=0";
						String wmsflag = (String) dao.executeQuery(sql_wmsid, new ColumnProcessor());
						if(wmsflag!=null) {
							returnJson.setResultBillcode(wmsflag);
							returnJson.setReturnMessage("NC产成品入库单保存并签字成功！");
							returnJson.setStatus("1");
							return RestUtils.toJSONString(returnJson);
//							throw new Exception("ID为"+wmsID+"的WMS单据已在NC生成过产成品入库单，重复推单，请检查！");
						}		
					}
					String userID = jsonAy.getString("userID");  //制单人ID
					String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+userID+"'";
					String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
					if(cuserid==null)
						throw new Exception("制单人"+userID+"在NC不存在，请检查！");
					String approver = jsonAy.getString("approverID");  //审批人ID
					String sql_approver = "select u.cuserid from sm_user u where u.pk_psndoc='"+approver+"'";
					String approverid = (String) dao.executeQuery(sql_approver, new ColumnProcessor());
					if(approverid==null)
						throw new Exception("审批人"+approver+"在NC不存在，请检查！");
					String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
					String vnote = jsonAy.getString("vnote");  //备注
					String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识

					String dmakedate = jsonAy.getString("dmakedate");  //制单日期
					UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
					UFDate dmakedate_d = dmakedate_t.getDate();	
					InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
					JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
					String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
					String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
					String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
					String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
					String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
					String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
					
//					String ctrantypeCode = jsonAy.getString("ctrantypeCode");  //出入库类型编码
					String cbizid = jsonAy.getString("cbizid");  //业务员ID
					String cwhsmanagerid = jsonAy.getString("cwhsmanagerid");  //库管员ID
					String pk_dept = jsonAy.getString("pk_dept");  //生产部门ID
					String pk_dept_v = queryOne("select v.pk_vid from org_dept_v v where v.pk_dept='"+pk_dept+"'");
					if(pk_dept!=null&&pk_dept_v==null)
						throw new Exception("部门ID"+pk_dept+"在NC不存在，请检查！");
//					String sql_trantype = "select t.pk_billtypeid from bd_billtype t where t.pk_billtypecode='"+ctrantypeCode+"' and t.pk_group='"+groupId+"'";
//					String ctrantypeid = (String) NCLocator.getInstance().lookup(IUAPQueryBS.class).executeQuery(sql_trantype, new ColumnProcessor());
					InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
					InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
					FinProdInVO mvo = new FinProdInVO();
					FinProdInHeadVO hvo= new FinProdInHeadVO();
					
					hvo.setCwarehouseid(cwarehouseid);
					hvo.setCbizid(cbizid);
					hvo.setCdptid(pk_dept);
					hvo.setCdptvid(pk_dept_v);
					hvo.setCorpoid(pk_org);
					hvo.setCorpvid(pk_org_v);
					hvo.setCprocalbodyoid(pk_org);
					hvo.setCprocalbodyvid(pk_org_v);
					hvo.setCprowarehouseid(cwarehouseid);
					hvo.setVdef1(wmsID);
					
					hvo.setCtrantypeid("0001A110000000002DXT");
					hvo.setCwhsmanagerid(cwhsmanagerid);
					hvo.setFbillflag(2);
					hvo.setIprintcount(0);
					hvo.setPk_group(pk_group);
					hvo.setPk_org(pk_org);
					hvo.setPk_org_v(pk_org_v);
					hvo.setVtrantypecode("46-01");
					hvo.setVnote(vnote);
					hvo.setStatus(VOStatus.NEW);
					mvo.setParentVO(hvo);
					FinProdInBodyVO[] MBV = new FinProdInBodyVO[list.size()];
					for (int s=0;s<list.size();s++){
						String cmaterialoid = list.getJSONObject(s).getString("cmaterialoid");  //物料id
						String ccostobject = list.getJSONObject(s).getString("ccostobject");  //成本对象（产成品ID）
//						String cprodprojectid = list.getJSONObject(s).getString("cprodprojectid");  //产成品辅助属性-项目
//						String cprojectid = list.getJSONObject(s).getString("cprojectid");  //项目
//						String cworkcenterid = list.getJSONObject(s).getString("cworkcenterid");  //工作中心
						String vbatchcode = list.getJSONObject(s).getString("vbatchcode");  //批次号
//						String clocationCode = list.getJSONObject(s).getString("clocationCode")==null?"null":list.getJSONObject(s).getString("clocationCode");  //货位编码
//						String clocationid = GetLocationid(cwarehouseid,clocationCode);
						String vproductbatch = list.getJSONObject(s).getString("vproductbatch");  //生产订单号
						String cstateid = list.getJSONObject(s).getString("cstateid");  //库存状态
						String cworkcenterid = list.getJSONObject(s).getString("cworkcenterid");  //工作中心
//						String vskucode = list.getJSONObject(s).getString("vskucode")==null?"":list.getJSONObject(s).getString("vskucode");  //特征码
						UFDouble nnum = list.getJSONObject(s).getString("nnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nnum"));   //主数量
						UFDouble nassistnum = list.getJSONObject(s).getString("nassistnum")==null?null:new UFDouble(list.getJSONObject(s).getString("nassistnum"));	//辅数量	
						String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
								+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
						HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
						String castunitid = "";
						String pk_source = "";
						String zdw = "";
						String measrate = "";
						if(materialMap!=null&&materialMap.get("zdw")!=null){
							castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
							pk_source=materialMap.get("pk_source").toString();
							zdw=materialMap.get("zdw").toString();
							measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
						}else
							throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
//						MaterialOutBodyVO bvo = MBV[s];
						FinProdInBodyVO bvo = new FinProdInBodyVO();
						bvo.setBbarcodeclose(UFBoolean.FALSE);
						bvo.setBcseal(UFBoolean.FALSE);
						bvo.setBonroadflag(UFBoolean.FALSE);
						bvo.setBreworkflag(UFBoolean.FALSE);
						bvo.setCastunitid(castunitid);
						bvo.setCbodytranstypecode("46-01");
						bvo.setCbodywarehouseid(cwarehouseid);
						bvo.setCmaterialoid(cmaterialoid);
						bvo.setCmaterialvid(pk_source);
						bvo.setCorpoid(pk_org);
						bvo.setCorpvid(pk_org_v);
						bvo.setCproductid(ccostobject);
						bvo.setCrowno(String.valueOf(s)+"0");
						bvo.setCstateid(cstateid);
						bvo.setCunitid(zdw);
						bvo.setCworkcenterid(cworkcenterid);
						bvo.setFlargess(UFBoolean.FALSE);
						bvo.setFproductclass(1);
						bvo.setNassistnum(nassistnum);
						bvo.setNnum(nnum);
						bvo.setNshouldassistnum(nassistnum);
						bvo.setNshouldnum(nnum);
						bvo.setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
						bvo.setPk_group(pk_group);
						bvo.setPk_org(pk_org);
						bvo.setPk_org_v(pk_org_v);
						bvo.setVbatchcode(vbatchcode);
						bvo.setVchangerate(measrate);
						bvo.setVproductbatch(vproductbatch);
						bvo.setDproducedate(dmakedate_d);
						bvo.setDbizdate(dmakedate_d);
						bvo.setDvalidate(getDvalidate(cmaterialoid,pk_org,dmakedate_d));
						MBV[s] = bvo;
						MBV[s].setStatus(VOStatus.NEW);
					}

					mvo.setChildrenVO(MBV);
					result = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, mvo, null, null);
					successMessage = "NC产成品入库单保存";
					if(sighFlag.equals("Y")){
						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						String taudittime = jsonAy.getString("taudittime");  //签字日期
						UFDateTime taudittime_t = new UFDateTime(taudittime);			
						InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
						InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
						FinProdInVO[] result2 = (FinProdInVO[])pfaction.processAction("SIGN", "46", null, result[0], null, null);
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
					if(result!=null&&result.length>0){
						String pk = result[0].getParentVO().getCgeneralhid();
						FinProdInVO errorVo = IQ.querySingleBillByPk(FinProdInVO.class, pk);
						try {  //回滚生成的单据
//							InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
							pfaction.processAction("DELETE", "46", null, errorVo, null, null);
						} catch (BusinessException e1) {
							// TODO 自动生成的 catch 块
							e1.printStackTrace();
						}
					}
				}
				return RestUtils.toJSONString(returnJson);
	 }
	 
	 public JSONString Generate46_oth(JSONObject jsonAy) {
			returnvo returnJson = new returnvo();
			String successMessage = "";
			FinProdInVO[] FP_origin = null;
			AggWrVO[] WV = null;
			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			IPwrMaintainService IPM = (IPwrMaintainService) NCLocator.getInstance().lookup(IPwrMaintainService.class);
			IPMOBusinessService PmoService = (IPMOBusinessService) NCLocator.getInstance().lookup(IPMOBusinessService.class);
			try {
				String wmsID = jsonAy.getString("wmsID");  
				if(wmsID!=null) {
					String sql_wmsid = "select h.vbillcode from ic_finprodin_h h where h.vdef1='"+wmsID+"' and h.dr=0";
					String wmsflag = (String) dao.executeQuery(sql_wmsid, new ColumnProcessor());
					if(wmsflag!=null) {
						returnJson.setResultBillcode(wmsflag);
						returnJson.setReturnMessage("NC产成品入库单保存并签字成功！");
						returnJson.setStatus("1");
						return RestUtils.toJSONString(returnJson);
//						throw new Exception("ID为"+wmsID+"的WMS单据已在NC生成过产成品入库单，重复推单，请检查！");
					}		
				}
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

				String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
//				String replenishflag = jsonAy.getString("replenishflag");  //是否退库
//				UFBoolean replenishflag2 = UFBoolean.FALSE;
//				if(replenishflag!=null&&replenishflag.equals("Y"))
//					replenishflag2 = UFBoolean.TRUE;
				String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
				UFDate dmakedate_d = dmakedate_t.getDate();	
				InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
				String cpmohid = jsonAy.getString("cpmohid");  //生产订单ID
				String scddlx = queryOne("select h.ctrantypeid from mm_pmo h where h.cpmohid='"+cpmohid+"'");  //生产订单类型
				String cplanoutputid = jsonAy.getString("cplanoutputid");  //副产品表体ID
				String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
				String sql_group = "select p.pk_group from mm_pmo p where p.cpmohid='"+cpmohid+"'";
				String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
				InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
				InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
						
				UFDouble nbwrastnum = jsonAy.getString("nbwrastnum")==null?UFDouble.ZERO_DBL:new UFDouble(jsonAy.getString("nbwrastnum"));  //完工数量
				UFDouble nbwrnum = jsonAy.getString("nbwrnum")==null?UFDouble.ZERO_DBL:new UFDouble(jsonAy.getString("nbwrnum"));  //完工主数量
				String batchcode = jsonAy.getString("batchcode");  //入库批次号
				String vbatchcodenote = jsonAy.getString("vbatchcodenote");  //批次备注
				String cstateid = jsonAy.getString("cstateid");
				String[] pks = {cpmohid};
				//IPMOQueryService
				IPMOQueryService IPQ = (IPMOQueryService) NCLocator.getInstance().lookup(IPMOQueryService.class);
				AbstractBill[] PV=(AbstractBill[])IPQ.queryByPks(pks);
				if (PV==null||PV.length==0){
					throw new Exception("传输的生产订单ID在系统不存在或者已删除!");
				}
				PMOAggVO pmovo = (PMOAggVO)PV[0];
				PMOAggVO[] pmos = {pmovo};
				PMOItemVO[] piv = (PMOItemVO[])PV[0].getChildrenVO();
				List<PMOItemVO> bodylist = new ArrayList();
				List<PMOPlanOutputVO> opv = new ArrayList();
				String fitemstatus = String.valueOf(piv[0].getAttributeValue("fitemstatus"));
				if(fitemstatus.equals("4")) {
					PmoService.put(pmos);
				}
				for(int w=0;w<piv.length;w++) {
					PMOPlanOutputVO[] ppos = piv[w].getPlanoutputs();
					if(ppos==null)
						continue;
					for(int r=0;r<ppos.length;r++) {
						String pid = ppos[r].getCplanoutputid();
						if(cplanoutputid.equals(pid)) {
							opv.add(ppos[r]);
							bodylist.add(piv[w]);
						}
					}
				}
				if(opv==null||opv.size()==0)
					throw new Exception("传输的联幅产品ID在NC不存在，请检查！");
				PV[0].setChildrenVO(bodylist.toArray(new PMOItemVO[bodylist.size()]));
				WV = (AggWrVO[])PfUtilTools.runChangeDataAry("55A2","55A4", PV);
				WV[0].getParentVO().setFbillstatus(1);
				WrItemVO[] WI = WV[0].getChildrenVO();
				PMOPlanOutputVO lfcpvo = opv.get(0);

				WI[0].setVbrowno("10");
				WI[0].setCbastunitid(lfcpvo.getCastunitid());
				WI[0].setCbbomversionid("~");
				WI[0].setCbmainbomid("");
				WI[0].setCbmaterialid(lfcpvo.getCmaterialid());
				WI[0].setCbmaterialvid(lfcpvo.getCmaterialvid());
				WI[0].setCbmobid(lfcpvo.getCplanoutputid());
				WI[0].setCbworkmanid("~");
				WI[0].setFbproducttype(lfcpvo.getFoutputtype());
				WI[0].setNbwrastnum(nbwrastnum);
				WI[0].setNbwrnum(nbwrnum);
				WI[0].setVbinbatchcode(batchcode);
				WI[0].setVbbomversioncode("");
				WI[0].setVbfirstrowid(lfcpvo.getCplanoutputid());
				WI[0].setVbsrcrowid(lfcpvo.getCplanoutputid());
				WI[0].setTbstarttime(new UFDateTime(System.currentTimeMillis()));
				WI[0].setTbendtime(new UFDateTime(System.currentTimeMillis()+1));
				WV = IPM.insert(WV);
				AggWrVO[] WV2 = (AggWrVO[])pfaction.processAction("APPROVE", "55A4", null, WV[0], null, null);
//				FinProdInVO[] pv = (FinProdInVO[])pfaction.processAction("55A4", "46", null, WV2[0], null, null);
				FinProdInVO pv = (FinProdInVO)PfUtilTools.runChangeData("55A4", "46", WV2[0]);
				pv.getHead().setStatus(VOStatus.NEW);
				pv.getHead().setCwarehouseid(cwarehouseid);
				pv.getHead().setCprowarehouseid(cwarehouseid);
				pv.getHead().setVtrantypecode("46-01");
				pv.getHead().setCtrantypeid("0001A110000000002DXT");
				pv.getHead().setVdef1(wmsID);
				FinProdInBodyVO pbv = pv.getBody(0);
				if(scddlx!=null&&scddlx.equals("0001A110000000002DZI")) {  //当生产订单类型为返工流程生产订单时，产成品入库也设为该类型
					pv.getHead().setVtrantypecode("46-Cxx-10");
					pv.getHead().setCtrantypeid("1001A2100000002LVM5K");
					pbv.setBreworkflag(UFBoolean.TRUE);
				}else {
					pv.getHead().setVtrantypecode("46-01");
					pv.getHead().setCtrantypeid("0001A110000000002DXT");	
				}
				String zcp = queryOne("select o.cmaterialid from mm_mo_planoutput p inner join mm_mo o on p.cmoid=o.cmoid where p.cplanoutputid='"+lfcpvo.getCplanoutputid()+"'");
				pbv.setNassistnum(pbv.getNshouldassistnum());
				pbv.setNnum(pbv.getNshouldnum());
				pbv.setVbatchcode(batchcode);
				pbv.setPk_batchcode(queryPK_batchcode(batchcode,pbv.getCmaterialoid()));
				pbv.setVbatchcodenote(vbatchcodenote);
				pbv.setCstateid(cstateid);
				
				pbv.setCproductid(zcp);
//				pbv.setCstateid("1001A210000000070OZ8");  //库存状态
				pbv.setCbodywarehouseid(cwarehouseid);
				pbv.setDproducedate(dmakedate_d);
				pbv.setDbizdate(dmakedate_d);
				pbv.setDvalidate(getDvalidate(pbv.getCmaterialoid(),pv.getHead().getPk_org(),dmakedate_d));
				pbv.setStatus(VOStatus.NEW);
			    FP_origin = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, pv, null, null); 
				successMessage = "NC产成品入库单保存";
				if(sighFlag.equals("Y")) {
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "46", null, FP_origin[0],null, null);
					successMessage = successMessage+"并签字";
				}
				successMessage = successMessage+"成功！";
				returnJson.setResultBillcode(FP_origin[0].getParentVO().getVbillcode());
				returnJson.setReturnMessage(successMessage);
				returnJson.setStatus("1");
			} catch (Exception e) {
				// TODO 自动生成的 catch 块
				returnJson.setResultBillcode("");
				returnJson.setStatus("0");
				returnJson.setReturnMessage(e.toString());
				if(FP_origin!=null&&FP_origin.length>0){
					String pk = FP_origin[0].getParentVO().getCgeneralhid();
					FinProdInVO errorVo = IQ.querySingleBillByPk(FinProdInVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "46", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
				if(WV!=null&&WV.length>0){
					String pk = WV[0].getParentVO().getPk_wr();
					AggWrVO errorVo = IQ.querySingleBillByPk(AggWrVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("UNAPPROVE", "55A4", null, errorVo, null, null);
						pfaction.processAction("DELETE", "55A4", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(returnJson);
	 }
	 
	 public JSONString Generate4Y(JSONObject jsonAy) {
		 returnvo returnJson = new returnvo();
		 Map ret = new HashMap();
		 TransOutVO[] result = null;
			try {
				NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));	
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
				
				String pkOders = jsonAy.getString("cbillid");  //调拨订单表头id数组
				String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库id
				String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
				JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
				String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
				String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
				String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
				InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
				InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
				InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
				String[] pks = null;
				if (pkOders != null && pkOders.length()>2){
					pkOders = pkOders.substring(1, pkOders.length()-1);
					pks = pkOders.split(",");
				}else
					throw new Exception("调拨订单ID属性为空，请检查传输参数！");
				IBillQueryService IBQ = (IBillQueryService)NCLocator.getInstance().lookup(IBillQueryService.class);
				AbstractBill[] SVO = IBQ.queryAbstractBillsByPks(BillVO.class, pks);
				if (SVO==null||SVO[0]==null){
					throw new Exception("传输的调拨订单ID在NC系统中不存在");
				}
//				for(int s=0;s<SVO.length;s++) {
//					BillItemVO[] bhis = (BillItemVO[])SVO[s].getChildrenVO();
//					for(int t=0;t<bhis.length;t++) {
//						bhis[t].setCoutstordocid(cwarehouseid);
//					}
//				}
				//单据转换
				TransOutVO[] transOuts = (TransOutVO[])PfUtilTools.runChangeDataAry("5X", "4Y", SVO);
				for(TransOutVO transOut : transOuts){
					transOut.getParentVO().setCwarehouseid(cwarehouseid);
					transOut.getParentVO().setStatus(2);
					TransOutBodyVO[] bodys = (TransOutBodyVO[]) transOut.getChildrenVO();
//					TransOutBodyVO[] mbvs = new TransOutBodyVO[list.size()];
					List<TransOutBodyVO > mbvs = new ArrayList<TransOutBodyVO>();
					for (int i = 0; i < list.size(); i++){
						String cbill_bid = list.getJSONObject(i).getString("cbill_bid") == null ? "null" : list.getJSONObject(i).getString("cbill_bid");
						UFDouble nnum = list.getJSONObject(i).getString("nnum") == null ? null : new UFDouble(list.getJSONObject(i).getString("nnum"));
						UFDouble nastnum = list.getJSONObject(i).getString("nastnum") == null ? null : new UFDouble(list.getJSONObject(i).getString("nastnum"));				
						String vbatchcode = list.getJSONObject(i).getString("vbatchcode") == null ? "null" : list.getJSONObject(i).getString("vbatchcode");
						String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"":list.getJSONObject(i).getString("clocationCode");  //货位编码
						String clocationid = GetLocationid(cwarehouseid,clocationCode);
						String cstateid = list.getJSONObject(i).getString("cstateid");
//						String cvmivenderid = list.getJSONObject(i).getString("cvmivenderid")==null?"":list.getJSONObject(i).getString("cvmivenderid");  //寄存供应商id
						
						//循环匹配表体
						for (int s = 0; s < bodys.length; s++){
							//将对应的主数量、批次号等进行填充
							if(cbill_bid.equals(bodys[s].getCsourcebillbid())){
								bodys[s].setVbatchcode(vbatchcode);
								bodys[s].setPk_batchcode(queryPK_batchcode(vbatchcode,bodys[s].getCmaterialoid()));
								bodys[s].setClocationid(clocationid);
								bodys[s].setDbizdate(new UFDate(dmakedate));
								bodys[s].setDvalidate(getDvalidate(bodys[s].getCmaterialoid(),transOut.getParentVO().getPk_org(),new UFDate(dmakedate)));
								bodys[s].setDproducedate(new UFDate(dmakedate));
								bodys[s].setNassistnum(nastnum);
								bodys[s].setNnum(nnum);
								bodys[s].setNshouldassistnum(nastnum);
								bodys[s].setNshouldnum(nnum);
								bodys[s].setCstateid(cstateid);
								TransOutBodyVO newvo = (TransOutBodyVO)bodys[s].clone();
								newvo.setCrowno(String.valueOf(i)+"0");
								newvo.setStatus(2);
								mbvs.add(newvo);
								break;
							}
						}
					}
					if(mbvs==null||mbvs.size()==0) {
						continue;
					}
					transOut.setChildrenVO(mbvs.toArray(new TransOutBodyVO[mbvs.size()]));
					//调拨出库单
					result = (TransOutVO[])pfaction.processAction("WRITE", "4Y", null, transOut, null, null);
					String resultBillcode = new String(result[0].getParentVO().getVbillcode());
					
					String successMessage = "NC调拨出库单保存";
					if(sighFlag.equals("Y")){
						String taudittime = jsonAy.getString("taudittime");  //签字日期
						UFDateTime taudittime_t = new UFDateTime(taudittime);			
						InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
						InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
						result = (TransOutVO[]) pfaction.processAction("SIGN", "4Y", null, result[0], null, null);
						successMessage = successMessage+"并签字";
					}
					successMessage = successMessage+"成功！";
					
					String[] cgeneralhIds = {result[0].getParentVO().getCgeneralhid()};
//					this.Generate_Media4E(cgeneralhIds,bodys);  //改了流程，签字自动生成下游单据了，所以不需在接口生成
//					returnJson.setResultBillcode(resultBillcode);
//					returnJson.setReturnMessage(successMessage);
//					returnJson.setStatus("1");	
					ret.put("ResultBillcode", resultBillcode);
					ret.put("ReturnMessage", successMessage);
					ret.put("Status","1");
					ret.put("cgeneralhid",result[0].getParentVO().getCgeneralhid());
					List<Map> mapList2 = new ArrayList();
					TransOutBodyVO[] bvos = result[0].getBodys();
					for(int k=0;k<bvos.length;k++) {
						String csourcebid = bvos[k].getCsourcebillbid();
						String cgeneralbid = bvos[k].getCgeneralbid();
						String Vbatchcode = bvos[k].getVbatchcode();
						Map map_msv = new HashMap();
						map_msv.put("cbill_bid", csourcebid);
						map_msv.put("cgeneralbid", cgeneralbid);
						map_msv.put("vbatchcode", Vbatchcode);
						mapList2.add(map_msv);
					}
					ret.put("ids",mapList2);
				}
			} catch (Exception e) {
				ret.put("ResultBillcode", "");
				ret.put("ReturnMessage", e.toString());
				ret.put("Status","0");
				if(result!=null&&result.length>0){
					String pk = result[0].getParentVO().getCgeneralhid();
					TransOutVO errorVo = IQ.querySingleBillByPk(TransOutVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "4Y", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(ret);
	 }
	 
     public JSONString Generate4E(JSONObject jsonAy) {
    	returnvo returnJson = new returnvo();
    	TransInVO[] result = null;
 		try {
 			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
 			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));	
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
 			
 			String cgeneralhids = jsonAy.getString("cgeneralhid");  //调拨出库单表头id数组
 			String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库id
 			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
 			JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
 			String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
 			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
 			if(pk_group==null)
 				throw new Exception("传入的仓库ID:"+cwarehouseid+"在NC不存在，请检查！");
 			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
 			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
 			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
 			String[] pks = null;
 			if (cgeneralhids != null && cgeneralhids.length()>2){
 				cgeneralhids = cgeneralhids.substring(1, cgeneralhids.length()-1);
 				pks = cgeneralhids.split(",");
 			}else
 				throw new Exception("调拨出库单ID属性为空，请检查传输参数！");
 			IBillQueryService IBQ = (IBillQueryService)NCLocator.getInstance().lookup(IBillQueryService.class);
 			AbstractBill[] SVO = IBQ.queryAbstractBillsByPks(TransOutVO.class, pks);
 			if (SVO==null||SVO[0]==null){
 				throw new Exception("传输的调拨出库单ID在NC系统中不存在");
 			}
 			//单据转换
 			TransInVO[] transIns = (TransInVO[])PfUtilTools.runChangeDataAry("4Y", "4E", SVO);
 			for(TransInVO transIn : transIns){
 				transIn.getParentVO().setCwarehouseid(cwarehouseid);
 				transIn.getParentVO().setStatus(2);
 				TransInBodyVO[] bodys = (TransInBodyVO[]) transIn.getChildrenVO();
 				TransInBodyVO[] mbvs = new TransInBodyVO[list.size()];
 				for (int i = 0; i < list.size(); i++){
 					String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid") == null ? "null" : list.getJSONObject(i).getString("cgeneralbid");
 					UFDouble nnum = list.getJSONObject(i).getString("nnum") == null ? null : new UFDouble(list.getJSONObject(i).getString("nnum"));
 					UFDouble nastnum = list.getJSONObject(i).getString("nastnum") == null ? null : new UFDouble(list.getJSONObject(i).getString("nastnum"));				
 					String vbatchcode = list.getJSONObject(i).getString("vbatchcode") == null ? "null" : list.getJSONObject(i).getString("vbatchcode");
 					String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"null":list.getJSONObject(i).getString("clocationCode");  //货位编码
 					String clocationid = GetLocationid(cwarehouseid,clocationCode);
 					String cstateid = list.getJSONObject(i).getString("cstateid");
// 					String cvmivenderid = list.getJSONObject(i).getString("cvmivenderid")==null?"":list.getJSONObject(i).getString("cvmivenderid");  //寄存供应商id
 					//循环匹配表体
 					for (int s = 0; s < bodys.length; s++){
 						//将对应的主数量、批次号等进行填充
 						if(cgeneralbid.equals(bodys[s].getCsourcebillbid())){
 							bodys[s].setVbatchcode(vbatchcode);
 							bodys[s].setDproducedate(dmakedate_t.getDate());
 							bodys[s].setDvalidate(getDvalidate(bodys[s].getCmaterialoid(),transIn.getParentVO().getPk_org(),new UFDate(dmakedate)));
 							bodys[s].setPk_batchcode(queryPK_batchcode(vbatchcode,bodys[s].getCmaterialoid()));
 							bodys[s].setClocationid(clocationid);
 							bodys[s].setDbizdate(dmakedate_t.getDate());
 							bodys[s].setNassistnum(nastnum);
 							bodys[s].setNnum(nnum);
 							bodys[s].setNshouldassistnum(nastnum);
 							bodys[s].setNshouldnum(nnum);
 							bodys[s].setCstateid(cstateid);
 							mbvs[i] = (TransInBodyVO)bodys[s].clone();
 							mbvs[i].setCrowno(String.valueOf(i)+"0");
 							mbvs[i].setStatus(2);
 							break;
 						}
 					}
 				}
 				if(mbvs[0]==null) {
 					continue;
 				}
 				transIn.setChildrenVO(mbvs);
 				//调拨出库单
 				result = (TransInVO[])pfaction.processAction("WRITE", "4E", null, transIn, null, null);
 				String resultBillcode = result[0].getParentVO().getVbillcode();
 				
 				String successMessage = "NC调拨入库单保存";
 				if(sighFlag.equals("Y")){
 					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
 					result = (TransInVO[]) pfaction.processAction("SIGN", "4E", null, result[0], null, null);
 					successMessage = successMessage+"并签字";
 				}
 				successMessage = successMessage+"成功！";
 				returnJson.setResultBillcode(resultBillcode);
 				returnJson.setReturnMessage(successMessage);
 				returnJson.setStatus("1");	
 			}
 		} catch (Exception e) {
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 			if(result!=null&&result.length>0){
				String pk = result[0].getParentVO().getCgeneralhid();
				TransInVO errorVo = IQ.querySingleBillByPk(TransInVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4E", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
 		}
 		return RestUtils.toJSONString(returnJson);
	 }

     public JSONString Generate4K(JSONObject jsonAy) {
 		// TODO 自动生成的方法存根
 		//构造表头 start 
    	returnvo returnJson = new returnvo();
    	String resultpk = "";
 		try {
 			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
 			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));	
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

 			String outWarehouseid = jsonAy.getString("outWarehouseid");  //仓库id
 			String inWarehouseid = jsonAy.getString("inWarehouseid");
 			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
 			String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+outWarehouseid+"'";
 			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
 			String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+outWarehouseid+"'";
			String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
			String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
			String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
			if(pk_org==null)
				throw new Exception("出库仓库ID"+outWarehouseid+"在NC不存在，请检查！");
			String pk_org2 = (String)dao.executeQuery("select s.pk_org from bd_stordoc s where s.pk_stordoc='"+inWarehouseid+"'", new ColumnProcessor());
			if(pk_org2==null)
				throw new Exception("入库仓库ID"+inWarehouseid+"在NC不存在，请检查！");
 			
 			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);			
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
 			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
 			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
 			
 			WhsTransBillVO billvo = new WhsTransBillVO();
 			WhsTransBillHeaderVO head = new WhsTransBillHeaderVO();
 			head.setPk_group(pk_group);
 			head.setVtrantypecode("4K-01");
 			head.setCtrantypeid("0001A110000000002DYV");
 			head.setBillmaker(cuserid);//制单人
 			head.setFbillflag(1);
 			head.setCreator(cuserid);
 			head.setVnote(jsonAy.getString("vnote"));

 			head.setCwarehouseid(outWarehouseid);   //出库仓库
 			head.setPk_org(pk_org);
 			head.setPk_org_v(pk_org_v);
 			head.setCorpoid(pk_org);
 			head.setCorpvid(pk_org_v);
 			head.setVdef1("Y");  //由WMS生成标识
 			head.setStatus(VOStatus.NEW);
 			head.setCotherwhid(inWarehouseid);  //入库仓库
 			if(null!=jsonAy.get("outDptid")){//出库部门
 				head.setCdptid(jsonAy.getString("outDptid"));
 				String cdptvid = (String)dao.executeQuery("select v.pk_vid from org_dept d inner join org_dept_v v on d.pk_dept=v.pk_dept where d.pk_dept='"+jsonAy.getString("outDptid")+"'", new ColumnProcessor());
 				head.setCdptvid(cdptvid);
 			}
 			String cauditorid = cuserid;
 			if(null!=jsonAy.get("cbizid")){//出库业务员
 				String ckywy = jsonAy.getString("cbizid");
 				head.setCbizid(ckywy);
 				String sql_cauditorid = "select u.cuserid from sm_user u where u.pk_psndoc='"+ckywy+"'";
  	 			cauditorid = (String) dao.executeQuery(sql_cauditorid, new ColumnProcessor());
  	 			if(cauditorid==null)
  	 				throw new Exception("出库业务员(转出人)"+ckywy+"未有NC账号或者NC账号未绑定身份人员，请检查！");
  	 			head.setCauditorid(cauditorid);  //转出人
 			}
 			
 			if(null!=jsonAy.get("inDptid")){//入库部门
 				head.setCotherdptid(jsonAy.getString("inDptid"));
 				String cdptvid2 = (String)dao.executeQuery("select v.pk_vid from org_dept d inner join org_dept_v v on d.pk_dept=v.pk_dept where d.pk_dept='"+jsonAy.getString("inDptid")+"'", new ColumnProcessor());
 				head.setCotherdptvid(cdptvid2);
 			}
 			String vadjuster = cuserid;
 			if(null!=jsonAy.get("cotherbizid")){//入库业务员
 				String rkywy = jsonAy.getString("cotherbizid");
 				head.setCotherbizid(jsonAy.getString("cotherbizid"));
  				String sql_vadjuster = "select u.cuserid from sm_user u where u.pk_psndoc='"+rkywy+"'";
  	 			vadjuster = (String) dao.executeQuery(sql_vadjuster, new ColumnProcessor());
  	 			if(vadjuster==null)
  	 				throw new Exception("入库业务员(转入人)"+rkywy+"未有NC账号或者NC账号未绑定身份人员，请检查！");
  	 			head.setVadjuster(vadjuster);  //转入人
 			}
 			//应到货日期
 			if(null !=jsonAy.get("arrivedate")){
 				head.setDshldarrivedate(new UFDate(jsonAy.getString("arrivedate")));
 			}else{
 				head.setDshldarrivedate(new UFDate());
 			}
 			//应发货日期 
 			if(null !=jsonAy.get("diliverdate")){
 				head.setDshlddiliverdate(new UFDate(jsonAy.getString("diliverdate")));
 			}else
 				head.setDshlddiliverdate(new UFDate());
 			
 			//构造表头 end 
 			//构造表体 start 
 			JSONArray bodyitems = jsonAy.getJSONArray("list");  //获取表体记录
 			List<WhsTransBillBodyVO > bodyvolist = new ArrayList<WhsTransBillBodyVO>();
 			int crowno = 1;
// 			for (Object objbillitem : bodyitems) {
 			for (int r=0;r<bodyitems.size();r++) {
// 				JSONObject billitem = (JSONObject) objbillitem;
 				WhsTransBillBodyVO bodyvo = new WhsTransBillBodyVO();
 				bodyvo.setCorpoid(pk_org);
 				bodyvo.setCorpvid(pk_org_v);
 				String cmaterialoid = bodyitems.getJSONObject(r).getString("cmaterialoid");  //物料id
 				String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
						+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
				HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
				String castunitid = "";
				String pk_source = "";
				String zdw = "";
				String measrate = "";
				String clocationid = bodyitems.getJSONObject(r).getString("clocationid");  //货位id
				if(materialMap!=null&&materialMap.get("zdw")!=null){
					castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
					pk_source=materialMap.get("pk_source").toString();
					zdw=materialMap.get("zdw").toString();
					measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
				}else
					throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
				
 				bodyvo.setCmaterialoid(cmaterialoid);
 				bodyvo.setCmaterialvid(pk_source);
 				bodyvo.setCunitid(zdw);
 				bodyvo.setCbodywarehouseid(outWarehouseid);
 				bodyvo.setCastunitid(castunitid);
				bodyvo.setVchangerate(measrate);
				bodyvo.setCstateid(bodyitems.getJSONObject(r).getString("cstateid"));
 				bodyvo.setNnum(new UFDouble(bodyitems.getJSONObject(r).getString("nnum")));
 				bodyvo.setNassistnum(new UFDouble(bodyitems.getJSONObject(r).getString("nassistnum")));
 				//bodyvo.setNassistnum(UFDoubleUtils.div(bodyvo.getNnum() ,numchangerate, 2));//辅数量 
// 				UFDouble numchangerate =  UFDoubleUtils.div(new UFDouble(bodyvo.getVchangerate().split("/")[0]),new UFDouble(bodyvo.getVchangerate().split("/")[1]));
 				bodyvo.setVnotebody(bodyitems.getJSONObject(r).getString("vbodynote"));//行备注
 				bodyvo.setClocationid(clocationid);
 				bodyvo.setCrowno((10*crowno)+"");
 				bodyvo.setStatus(VOStatus.NEW);
 				crowno++;
 				//批次处理 start
 				String vbatchcode = null==bodyitems.getJSONObject(r).get("vbatchcode")?"":bodyitems.getJSONObject(r).getString("vbatchcode");
 				bodyvo.setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
 				bodyvo.setVbatchcode(vbatchcode);
// 				if(!PubAppTool.isNull(vbatchcode)){//都不为空，组合
// 					String ncbatchcode = vbatchcode;
// 						BatchcodeVO batchvo = querdmo.QueryBatchbyMaterialBatchNO(bodyvo.getCmaterialoid(), ncbatchcode);
// 						bodyvo.setVbatchcode(ncbatchcode);
// 						if(null!=batchvo){
// 							bodyvo.setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
// 							bodyvo.setCqualitylevelid(batchvo.getCqualitylevelid());
// 						}
// 				}
 				//批次处理 end
 				bodyvolist.add(bodyvo);
 			}
 			//构造表体 end 
 			
 			//组合单据vo
 			billvo.setParent(head);
 			billvo.setChildrenVO(bodyvolist.toArray(new WhsTransBillBodyVO[bodyvolist.size()]));
 			WhsTransBillVO[] result = (WhsTransBillVO[])pfaction.processAction("WRITE", "4K", null, billvo, null,null);
 			resultpk = result[0].getParentVO().getCspecialhid();
 			WhsTransBillVO AP = IQ.querySingleBillByPk(WhsTransBillVO.class, resultpk);
 			String billcode = AP.getParentVO().getVbillcode();
// 			String billcode = (String)dao.executeQuery("select vbillcode from ic_whstrans_h where cspecialhid='"+resultpk+"'", new ColumnProcessor());
 			String successMessage = "NC转库单保存";
			if(sighFlag.equals("Y")){
				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
				result = (WhsTransBillVO[]) pfaction.processAction("APPROVE", "4K", null, AP, null, null);
				successMessage = successMessage+"并签字";
			}
			Form4Kto4I(AP,null,cauditorid,dmakedate_t);
			From4Kto4A(AP,null,vadjuster,dmakedate_t);
			dao.executeUpdate("update ic_whstrans_h h set h.cauditorid='"+cauditorid+"',h.vadjuster='"+vadjuster+"' where h.cspecialhid='"+resultpk+"'");
			successMessage = successMessage+"成功！";
			returnJson.setResultBillcode(billcode);
			returnJson.setReturnMessage(successMessage);
			returnJson.setStatus("1");	
 		} catch (Exception e) {
 			if(!resultpk.equals("")&&resultpk.length()>0) {
 				WhsTransBillVO AP = IQ.querySingleBillByPk(WhsTransBillVO.class, resultpk);
 				int fbillflag = AP.getParentVO().getFbillflag();
 				try {
 					if(fbillflag==4){
 	 				   pfaction.processAction("UNAPPROVE", "4K", null, AP, null, null);
 	 				}
 	 				pfaction.processAction("DELETE", "4K", null, AP, null, null);
 				}catch (Exception e1) {
 					e1.printStackTrace();
 				}
 				
 			}	
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 		}
 		return RestUtils.toJSONString(returnJson);
     }
     
     //转库生成其他出库
     private void Form4Kto4I(WhsTransBillVO AP,JSONArray bodyitems,String cauditorid,UFDateTime makedate) throws Exception {
    	 GeneralOutVO PV = (GeneralOutVO)PfUtilTools.runChangeData("4K", "4I", AP);
    	 PV.getParentVO().setCtrantypeid("0001A110000000002DYJ");
    	 PV.getParentVO().setVtrantypecode("4I-02");
    	 PV.getParentVO().setBillmaker(cauditorid);
    	 PV.getParentVO().setCreator(cauditorid);
    	 PV.getParentVO().setApprover(cauditorid);
//    	 InvocationInfoProxy.getInstance().setUserId(cauditorid);
    	 GeneralOutBodyVO[] bodys = PV.getBodys();
    	 ArrayList<GeneralOutBodyVO> oi_new = new ArrayList<GeneralOutBodyVO>();
    	 if(bodyitems!=null) {
    		 for (int r=0;r<bodyitems.size();r++) {
    			 String cspecialbid = bodyitems.getJSONObject(r).getString("cspecialbid");
    			 for(int i=0;i<bodys.length;i++) {
    	    		 String sourcebillbid = bodys[i].getCsourcebillbid();
    	    		 if(sourcebillbid!=null&&sourcebillbid.equals(cspecialbid)) {
    	    			 GeneralOutBodyVO newitem = (GeneralOutBodyVO)bodys[i].clone();
    	    			 newitem.setNshouldnum(new UFDouble(bodyitems.getJSONObject(r).getString("nnum")));
    	    			 newitem.setNnum(new UFDouble(bodyitems.getJSONObject(r).getString("nnum")));
    	    			 newitem.setNshouldassistnum(new UFDouble(bodyitems.getJSONObject(r).getString("nassistnum")));
    	    			 newitem.setNassistnum(new UFDouble(bodyitems.getJSONObject(r).getString("nassistnum")));
    	    			 newitem.setVnotebody(bodyitems.getJSONObject(r).getString("vbodynote"));
   		  				 String vbatchcode = bodyitems.getJSONObject(r).getString("vbatchcode");
   		  			     newitem.setPk_batchcode(queryPK_batchcode(vbatchcode,newitem.getCmaterialoid()));
   		  			     newitem.setVbatchcode(vbatchcode);
   		  			     newitem.setCstateid(bodyitems.getJSONObject(r).getString("cstateid"));
    	    			 newitem.setCrowno(String.valueOf(r)+"0");
    	    			 newitem.setDproducedate(makedate.getDate());
    	    			 newitem.setDbizdate(makedate.getDate());
    	    			 newitem.setDvalidate(getDvalidate(bodys[i].getCmaterialoid(),PV.getParentVO().getPk_org(),new UFDate(System.currentTimeMillis())));
    	    			 oi_new.add(newitem);
    	    		 }
    	    	 }
    		 }
    		 PV.setChildrenVO(oi_new.toArray(new GeneralOutBodyVO[oi_new.size()]));
    	 }else {
    		 for(int i=0;i<bodys.length;i++) {
	    		 bodys[i].setCrowno(String.valueOf(i)+"0");
	    		 bodys[i].setDproducedate(makedate.getDate());
	    		 bodys[i].setDbizdate(makedate.getDate());
	    		 bodys[i].setDvalidate(getDvalidate(bodys[i].getCmaterialoid(),PV.getParentVO().getPk_org(),makedate.getDate()));
	    	 }
    	 }
    	 
    	 
    	 GeneralOutVO[] result = (GeneralOutVO[])pfaction.processAction("WRITE", "4I", null, PV, null, null);
    	 pfaction.processAction("SIGN", "4I", null, result[0], null, null);
    	 dao.executeUpdate("update ic_generalout_h h set h.approver='"+cauditorid+"' where h.cgeneralhid='"+result[0].getParentVO().getCgeneralhid()+"'");
    	 dao.executeUpdate("update ia_i7bill h set h.billmaker='"+cauditorid+"',h.creator='"+cauditorid+"',h.modifier='"+cauditorid+"' where h.cbillid in (select b.cbillid from ia_i7bill_b b where b.csrcid='"+result[0].getParentVO().getCgeneralhid()+"' and b.dr=0)");
     }
     
     //转库生成其他入库
     private void From4Kto4A(WhsTransBillVO AP,JSONArray bodyitems,String vadjuster,UFDateTime makedate) throws Exception {
    	 GeneralInVO PV = (GeneralInVO)PfUtilTools.runChangeData("4K", "4A", AP); 
    	 PV.getParentVO().setCtrantypeid("0001A110000000002DXW");
    	 PV.getParentVO().setVtrantypecode("4A-02");
    	 PV.getParentVO().setBillmaker(vadjuster);
    	 PV.getParentVO().setCreator(vadjuster);
    	 PV.getParentVO().setApprover(vadjuster);
//    	 InvocationInfoProxy.getInstance().setUserId(vadjuster);
    	 String pk_org = PV.getParentVO().getPk_org();
    	 String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
		 String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
		 PV.getParentVO().setPk_org_v(pk_org_v);
    	 GeneralInBodyVO[] bodys = PV.getBodys();
    	 ArrayList<GeneralInBodyVO> oi_new = new ArrayList<GeneralInBodyVO>();
    	 if(bodyitems!=null) {
    		 for (int r=0;r<bodyitems.size();r++) {
    			 String cspecialbid = bodyitems.getJSONObject(r).getString("cspecialbid");
    			 for(int i=0;i<bodys.length;i++) {
    				 String sourcebillbid = bodys[i].getCsourcebillbid();
    				 if(sourcebillbid!=null&&sourcebillbid.equals(cspecialbid)) {
    					 GeneralInBodyVO newitem = (GeneralInBodyVO)bodys[i].clone();
    					 newitem.setNnum(new UFDouble(bodyitems.getJSONObject(r).getString("nnum")));
    					 newitem.setNassistnum(new UFDouble(bodyitems.getJSONObject(r).getString("nassistnum")));
    					 newitem.setVnotebody(bodyitems.getJSONObject(r).getString("vbodynote"));
		  				 String vbatchcode = bodyitems.getJSONObject(r).getString("vbatchcode");
		  				 newitem.setPk_batchcode(queryPK_batchcode(vbatchcode,newitem.getCmaterialoid()));
		  				 newitem.setVbatchcode(vbatchcode);
		  				 newitem.setCstateid(bodyitems.getJSONObject(r).getString("cstateid"));
    					 newitem.setCrowno(String.valueOf(r)+"0");
    					 newitem.setDproducedate(makedate.getDate());
    					 newitem.setDbizdate(makedate.getDate());
    					 newitem.setDvalidate(getDvalidate(bodys[i].getCmaterialoid(),pk_org,makedate.getDate()));
                		 oi_new.add(newitem);
    				 }
            		 
            	 }
    		 }
    		 PV.setChildrenVO(oi_new.toArray(new GeneralInBodyVO[oi_new.size()]));
    	 }else {
    		 for(int i=0;i<bodys.length;i++) {
        		 bodys[i].setCrowno(String.valueOf(i)+"0");
        		 bodys[i].setDproducedate(new UFDate(System.currentTimeMillis()));
        		 bodys[i].setDvalidate(getDvalidate(bodys[i].getCmaterialoid(),pk_org,new UFDate(System.currentTimeMillis())));
        	 } 
    	 }
    	 GeneralInVO[] result = (GeneralInVO[])pfaction.processAction("WRITE", "4A", null, PV, null, null);
//    	 InvocationInfoProxy.getInstance().setUserId(vadjuster);
    	 pfaction.processAction("SIGN", "4A", null, result[0], null, null);
    	 dao.executeUpdate("update ic_generalin_h h set h.approver='"+vadjuster+"' where h.cgeneralhid='"+result[0].getParentVO().getCgeneralhid()+"'");
    	 dao.executeUpdate("update ia_i4bill h set h.billmaker='"+vadjuster+"',h.creator='"+vadjuster+"',h.modifier='"+vadjuster+"' where h.cbillid in (select b.cbillid from ia_i4bill_b b where b.csrcid='"+result[0].getParentVO().getCgeneralhid()+"' and b.dr=0)");
     }
     
     public JSONString Rewrite4K(JSONObject jsonAy) {
  		// TODO 自动生成的方法存根
     	returnvo returnJson = new returnvo();
     	String resultpk = null;
  		try {
  			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
  			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));	
 			String userID = jsonAy.getString("userID");  //用户ID
 			String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+userID+"'";
 			String cuserid = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
 			if(cuserid==null)
 				throw new Exception("制单人"+userID+"在NC不存在，请检查！");
  			
  			String updateTime = jsonAy.getString("updateTime");  //制单日期
 			UFDateTime dmakedate_t = new UFDateTime(updateTime);			
  			String cspecialhid = jsonAy.getString("cspecialhid");  //转库单表头ID
  			WhsTransBillVO AP = IQ.querySingleBillByPk(WhsTransBillVO.class, cspecialhid);
  			WhsTransBillHeaderVO head = AP.getHead();
  			WhsTransBillBodyVO[] bodys = AP.getBodys();
  			String pk_group = head.getPk_group();
  			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
  			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
  			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量	
  			if(head.getFbillflag()!=1) {
//  				throw new Exception("转库单状态不是未审批状态，请检查！");
  				WhsTransBillVO[] ff = (WhsTransBillVO[])pfaction.processAction("UNAPPROVE", "4K", null, AP, null, null);
  				AP = ff[0];
  			}
  			head.setVdef1("Y");  //由WMS生成标识
  			head.setVnote(jsonAy.getString("vnote"));
  			head.setStatus(VOStatus.UPDATED);
  			if(null!=jsonAy.get("outDptid")){//出库部门
  				head.setCdptid(jsonAy.getString("outDptid"));
  				String cdptvid = (String)dao.executeQuery("select v.pk_vid from org_dept d inner join org_dept_v v on d.pk_dept=v.pk_dept where d.pk_dept='"+jsonAy.getString("outDptid")+"'", new ColumnProcessor());
  				head.setCdptvid(cdptvid);
  			}
  			String cauditorid = cuserid;
  			if(null!=jsonAy.get("cbizid")){//出库业务员
  				String ckywy = jsonAy.getString("cbizid");
  				head.setCbizid(ckywy);
  				String sql_cauditorid = "select u.cuserid from sm_user u where u.pk_psndoc='"+ckywy+"'";
  	 			cauditorid = (String) dao.executeQuery(sql_cauditorid, new ColumnProcessor());
  	 			if(cauditorid==null)
  	 				throw new Exception("出库业务员(转出人)"+ckywy+"未有NC账号或者NC账号未绑定身份人员，请检查！");
  	 			head.setCauditorid(cauditorid);  //转出人
  			}
  			
  			if(null!=jsonAy.get("inDptid")){//入库部门
  				head.setCotherdptid(jsonAy.getString("inDptid"));
  				String cdptvid2 = (String)dao.executeQuery("select v.pk_vid from org_dept d inner join org_dept_v v on d.pk_dept=v.pk_dept where d.pk_dept='"+jsonAy.getString("inDptid")+"'", new ColumnProcessor());
  				head.setCotherdptvid(cdptvid2);
  			}
  			String vadjuster = cuserid;
  			if(null!=jsonAy.get("cotherbizid")){//入库业务员
  				String rkywy = jsonAy.getString("cotherbizid");
  				head.setCotherbizid(jsonAy.getString("cotherbizid"));
  				String sql_vadjuster = "select u.cuserid from sm_user u where u.pk_psndoc='"+rkywy+"'";
  	 			vadjuster = (String) dao.executeQuery(sql_vadjuster, new ColumnProcessor());
  	 			if(vadjuster==null)
  	 				throw new Exception("入库业务员(转入人)"+rkywy+"未有NC账号或者NC账号未绑定身份人员，请检查！");
  	 			head.setVadjuster(vadjuster);  //转入人
  			}
  			//应到货日期
  			if(null !=jsonAy.get("arrivedate")){
  				head.setDshldarrivedate(new UFDate(jsonAy.getString("arrivedate")));
  			}
  			//应发货日期 
  			if(null !=jsonAy.get("diliverdate")){
  				head.setDshlddiliverdate(new UFDate(jsonAy.getString("diliverdate")));
  			}

  			//构造表体 start 
  			JSONArray bodyitems = jsonAy.getJSONArray("list");  //获取表体记录
  			for (int r=0;r<bodyitems.size();r++) {
  				String cspecialbid = bodyitems.getJSONObject(r).getString("cspecialbid");
  				String clocationid = bodyitems.getJSONObject(r).getString("clocationid");  //货位id
  				for(int s=0;s<bodys.length;s++) {
  					String bpk = bodys[s].getCspecialbid();
  					if(bpk.equals(cspecialbid)) {
  						WhsTransBillBodyVO bodyvo = bodys[s];
  		 				String cmaterialoid = bodyvo.getCmaterialoid();
  		 				//因为转库数可能会超过备料数，如果在这里设置大于备料数的话会报错“转库数量总和不合法，回写失败！”，所以注释掉，在其他出入库方法里面设置数量
//  		  				bodyvo.setNnum(new UFDouble(bodyitems.getJSONObject(r).getString("nnum")));
//  		  				bodyvo.setNassistnum(new UFDouble(bodyitems.getJSONObject(r).getString("nassistnum")));
  		 				bodyvo.setStatus(VOStatus.UPDATED);
  		 				bodyvo.setClocationid(clocationid);
//  		  			    bodyvo.setVnotebody(bodyitems.getJSONObject(r).getString("vbodynote"));
//  		  				String vbatchcode = bodyitems.getJSONObject(r).getString("vbatchcode");
//  		  				bodyvo.setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
//  		  				bodyvo.setVbatchcode(vbatchcode);
//  		  				bodyvo.setCstateid(bodyitems.getJSONObject(r).getString("cstateid"));
  					}
  				}
  			}
  			//构造表体 end 
  			AP.setParentVO(head);
  			AP.setChildrenVO(bodys);
  			//组合单据vo
  			WhsTransBillVO[] result = (WhsTransBillVO[])pfaction.processAction("WRITE", "4K", null, AP, null,null);
  			resultpk = result[0].getParentVO().getCspecialhid();
  			WhsTransBillVO AP2 = IQ.querySingleBillByPk(WhsTransBillVO.class, resultpk);
  			String billcode = AP2.getParentVO().getVbillcode();
//  			String billcode = (String)dao.executeQuery("select vbillcode from ic_whstrans_h where cspecialhid='"+resultpk+"'", new ColumnProcessor());
  			String successMessage = "NC转库单保存";
  			int fb = AP2.getParentVO().getFbillflag();
  			if(fb==1)
  				result = (WhsTransBillVO[]) pfaction.processAction("APPROVE", "4K", null, AP2, null, null);
			successMessage = successMessage+"并签字";
			Form4Kto4I(AP2,bodyitems,cauditorid,dmakedate_t);
 			From4Kto4A(AP2,bodyitems,vadjuster,dmakedate_t);
 			dao.executeUpdate("update ic_whstrans_h h set h.cauditorid='"+cauditorid+"',h.vadjuster='"+vadjuster+"' where h.cspecialhid='"+resultpk+"'");
 			successMessage = successMessage+"成功！";
 			returnJson.setResultBillcode(billcode);
 			returnJson.setReturnMessage(successMessage);
 			returnJson.setStatus("1");	
  		} catch (Exception e) {
  			// TODO 自动生成的 catch 块
  			try {
  				String govid = queryOne("select b.cgeneralhid from ic_generalout_b b inner join ic_whstrans_h h on b.csourcebillhid=h.cspecialhid where h.cspecialhid='"+resultpk+"' and h.dr=0 and b.dr=0");
  				GeneralOutVO gov = IQ.querySingleBillByPk(GeneralOutVO.class, govid);
  				if(gov!=null) {
  	  			    int bf = gov.getParentVO().getFbillflag();
  	  				if(bf==4) {
  	  				   pfaction.processAction("CANCELSIGN", "4I", null, gov, null, null);
  	  				   pfaction.processAction("DELETE", "4I", null, gov, null, null);  
  	  				}else {
  	  				   pfaction.processAction("DELETE", "4I", null, gov, null, null); 
  	  				}
  	  			}
  				String givid = queryOne("select b.cgeneralhid from ic_generalin_b b inner join ic_whstrans_h h on b.csourcebillhid=h.cspecialhid where h.cspecialhid='"+resultpk+"' and h.dr=0 and b.dr=0");
  				GeneralInVO giv = IQ.querySingleBillByPk(GeneralInVO.class, givid);	
  				if(giv!=null) {
  					int bf = giv.getParentVO().getFbillflag();
  					if(bf==4) {
  						pfaction.processAction("CANCELSIGN", "4A", null, giv, null, null);
  		  				pfaction.processAction("DELETE", "4A", null, giv, null, null); 
  					}else {
  						pfaction.processAction("DELETE", "4A", null, giv, null, null);
  					}	
  				}
  			}catch (Exception e1) {
					e1.printStackTrace();
			}
  			
  			returnJson.setResultBillcode("");
  			returnJson.setStatus("0");
  			returnJson.setReturnMessage(e.toString());
  		}
  		return RestUtils.toJSONString(returnJson);
      
     }
     
     public JSONString Generate23(JSONObject jsonAy) {
//			returnvo returnJson = new returnvo();	
    	    JSONObject resultJson = new JSONObject();
			ArriveVO[] result = null;
			String billcode = "";
			Map ret = new HashMap();
			try {
				NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
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
				 String vnote = jsonAy.getString("vnote");  //备注
				 String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
				 String dmakedate = jsonAy.getString("dmakedate");  //制单日期
				 UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
				 UFDate dmakedate_d = dmakedate_t.getDate();	
				 InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
				 JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
				 InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
				 InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
				 String successMessage = "";
					
				String pk_order = jsonAy.getString("pk_order");  //采购订单表头id
				String[] pks = null;
				if (pk_order.length()>0){
					pks = pk_order.split(",");
				}else
					throw new Exception("单据ID属性为空，请检查传输参数！");
				IOrderQuery IQ = (IOrderQuery) NCLocator.getInstance().lookup(IOrderQuery.class);
				OrderVO[] po_data = IQ.queryOrderVOsByIds(pks, UFBoolean.FALSE);
				if (po_data==null||po_data.length==0){
					throw new Exception("采购订单ID在NC系统中不存在，请检查！");
				}
				
//				OrderItemVO[] oi_new = new OrderItemVO[list.length()];
				ArrayList<OrderItemVO> oi_new = new ArrayList<OrderItemVO>();
				for (int t=0;t<po_data.length;t++){
					OrderItemVO[] oi = (OrderItemVO[])po_data[t].getChildrenVO();
					for (int r=0;r<list.size();r++){
						String pk_order_b = list.getJSONObject(r).getString("pk_order_b")==null?"null":list.getJSONObject(r).getString("pk_order_b");  //采购订单表体ID
						UFDouble nnum = list.getJSONObject(r).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(r).getString("nnum"));  //到货主数量
//						UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));
						for (int s=0;s<oi.length;s++){
							String pk_order_b2=oi[s].getPk_order_b()==null?"null":oi[s].getPk_order_b();
							if(pk_order_b2.equals(pk_order_b)){
								oi[s].setNcanarrivenum(nnum);
								
								oi_new.add(oi[s]);
							}
						}
					}
				}
				po_data[0].setChildrenVO(oi_new.toArray(new OrderItemVO[list.size()]));
				OrderVO[] po_datas = {po_data[0]};

				ArriveVO[] AV = (ArriveVO[])PfUtilTools.runChangeDataAry("21", "23", po_datas);
				for(int r=0;r<AV.length;r++){
					ArriveItemVO[] bodys = AV[r].getBVO();
					ArriveItemVO[] newbodys = new ArriveItemVO[list.size()]; 
					for (int i=0;i<list.size();i++){
						String pk_order_b = list.getJSONObject(i).getString("pk_order_b")==null?"null":list.getJSONObject(i).getString("pk_order_b");  //采购订单表体ID
						UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(i).getString("nnum"));  //到货主数量
						UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));  //到货数量
						String clocationid = list.getJSONObject(i).getString("clocationid") == null ? "":list.getJSONObject(i).getString("clocationid");//货位
						for (int j=0;j<bodys.length;j++){
							String sourcebillbid = bodys[j].getCsourcebid()==null?"null":bodys[j].getCsourcebid();
							String firstbid = bodys[j].getCfirstbid()==null?"null":bodys[j].getCfirstbid();
//							String firstbid = pk_order_b;
							String firstid = InfoQuery(firstbid,"cfirstbillhid");  //重新查询获取源头单据表头ID
							String cgddh = InfoQuery(firstbid,"cgddh");  //重新查询获取源头单据号
							String notebody = bodys[j].getVmemob()==null?"null":bodys[j].getVmemob();
							
							if(pk_order_b.equals(sourcebillbid)){
								bodys[j].setNnum(nnum);
								bodys[j].setNastnum(nastnum);
								bodys[j].setCrowno(String.valueOf(i)+"0");
								
								bodys[j].setCfirstid(firstid);
								bodys[j].setCfirstbid(firstbid);
								bodys[j].setVfirstcode(cgddh);
								bodys[j].setPk_order(firstid);
								bodys[j].setPk_order_b(firstbid);
								bodys[j].setVsourcecode(cgddh);
								bodys[j].setPk_rack(clocationid);
//								bodys[j].setCsourceid(InfoQuery(bodys[j].getCsourcebid(),"csourceid_arriveorder"));
								newbodys[i]=bodys[j];	
								newbodys[i].setVmemob(notebody=="null"?"":notebody);
								break;
							}
						}
					}
					if(newbodys[0]==null){
						throw new Exception("传输的采购订单表体记录都不满足入库条件,请检查！");
					}
					AV[r].setChildrenVO(newbodys);
					result = (ArriveVO[])pfaction.processAction("SAVEBASE", "23", null, AV[r], null, null);
					billcode = (String)result[0].getParentVO().getAttributeValue("vbillcode");
					successMessage = "NC到货单保存";
					if(sighFlag.equals("Y")){
						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						String taudittime = jsonAy.getString("taudittime");  //签字日期
						UFDateTime taudittime_t = new UFDateTime(taudittime);			
						InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
						InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
						pfaction.processAction("APPROVE", "23", null, result[0], null, null);
						successMessage = successMessage+"并签字";
					}
		            updateSourceID(result[0]);
				}
				
				ret.put("ResultBillcode", billcode);
				ret.put("ReturnMessage", successMessage+"成功！");
				ret.put("Status","1");
				ret.put("pk_arriveorder",(String)result[0].getParentVO().getAttributeValue("pk_arriveorder"));
				List<Map> mapList2 = new ArrayList();
				ArriveItemVO[] bvos = result[0].getBVO();
				for(int k=0;k<bvos.length;k++) {
					String csourcebid = bvos[k].getCsourcebid();
					String pk_arriveorder_b = bvos[k].getPk_arriveorder_b();
					Map map_msv = new HashMap();
					map_msv.put("pk_order_b", csourcebid);
					map_msv.put("pk_arriveorder_b", pk_arriveorder_b);
					mapList2.add(map_msv);
				}
				ret.put("ids",mapList2);
//				returnJson.setResultBillcode(billcode);
//				returnJson.setReturnMessage(successMessage+"成功！");
//				returnJson.setStatus("1");
			} catch (Exception e) {
				ret.put("ResultBillcode", "");
				ret.put("ReturnMessage", e.toString());
				ret.put("Status","0");
				if(result!=null&&result.length>0){
					String pk = result[0].getHVO().getPk_arriveorder();
					ArriveVO errorVo = IQ.querySingleBillByPk(ArriveVO.class, pk);
					try {  //回滚生成的单据
//						InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
						pfaction.processAction("DELETE", "23", null, errorVo, null, null);
					} catch (BusinessException e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			}
			return RestUtils.toJSONString(ret);
     }
     
     private void updateSourceID(ArriveVO vo) throws DAOException{
     	ArriveItemVO[] bodys = vo.getBVO();
     	for(int i=0;i<bodys.length;i++){
     		String Pk_arriveorder_b = bodys[i].getPk_arriveorder_b();
     		String sourcebid= bodys[i].getCsourcebid();
     		String sql = "update po_arriveorder_b b set b.csourceid=(select pk_order from po_order_b where pk_order_b='"+sourcebid+"') where b.pk_arriveorder_b='"+Pk_arriveorder_b+"'";
     		dao.executeUpdate(sql);
     	}
     	
     }
     
     public JSONString Generate2345(JSONObject jsonAy) {
    	returnvo returnJson = new returnvo();
 		PurchaseInVO[] result = null;
 		String billcode = "";
 		try {
 			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
 			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
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
			 String vnote = jsonAy.getString("vnote");  //备注
			 String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			 String replenishflag = jsonAy.getString("replenishflag");  //是否退库
			 UFBoolean replenishflag2 = UFBoolean.FALSE;
			 if(replenishflag!=null&&replenishflag.equals("Y"))
				replenishflag2 = UFBoolean.TRUE;
			 String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			 String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
			 UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			 UFDate dmakedate_d = dmakedate_t.getDate();	
			 InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			 JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
			 InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
			 InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
			 String successMessage = "";
 			
 			String pk_arriveorder = jsonAy.getString("pk_arriveorder");  //到货单ID
 			String arriveorderCode = InfoQuery(pk_arriveorder,"arriveorderCode");
 			String[] acs = {arriveorderCode};
 			IArriveBillQuery IQ = (IArriveBillQuery) NCLocator.getInstance().lookup(IArriveBillQuery.class);
 			ArriveVO[] AV = IQ.queryArriveAggVo(acs);
 			
 			if (AV==null||AV.length==0){
 				throw new Exception("传输的到货单ID在系统不存在或者已删除!");
 			}
 			ArriveItemVO[] items = (ArriveItemVO[])AV[0].getChildrenVO();
 			ArriveItemVO[] items_new = new ArriveItemVO[list.size()];
 			UFDouble ntotalnum = UFDouble.ZERO_DBL;
 			for (int r=0;r<list.size();r++){
 				String pk_arriveorder_b = list.getJSONObject(r).getString("pk_arriveorder_b")==null?"null":list.getJSONObject(r).getString("pk_arriveorder_b");
 				UFDouble nnum = list.getJSONObject(r).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(r).getString("nnum"));
 				ntotalnum = ntotalnum.add(nnum);
 				for (int s=0;s<items.length;s++){
 					String Pk_arriveorder_b2=items[s].getPk_arriveorder_b()==null?"null":items[s].getPk_arriveorder_b();
 					if(Pk_arriveorder_b2.equals(pk_arriveorder_b)){
 						items_new[r]=items[s];
 					}
 				}
 			}
 			AV[0].setChildrenVO(items_new);
 			
 			PurchaseInVO[] PV = (PurchaseInVO[])PfUtilTools.runChangeDataAry("23","45", AV);
 			for (int c=0;c<PV.length;c++){
 				PV[c].getParentVO().setCwarehouseid(cwarehouseid);
 				PV[c].getParentVO().setVnote(vnote);
 				PV[c].getParentVO().setNtotalnum(ntotalnum);
 				PV[c].getParentVO().setCtrantypeid("0001A110000000002DXO");
				PV[c].getParentVO().setVtrantypecode("45-01");
				PV[c].getHead().setFreplenishflag(replenishflag2);
				PV[c].getParentVO().setStatus(VOStatus.NEW);
 				PurchaseInBodyVO[] bodys = PV[c].getBodys();
 				PurchaseInBodyVO[] newbodys = new PurchaseInBodyVO[bodys.length];  //重新组建表体VO
 				for (int i=0;i<list.size();i++){
 					String pk_arriveorder_b = list.getJSONObject(i).getString("pk_arriveorder_b")==null?"null":list.getJSONObject(i).getString("pk_arriveorder_b");
 					UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(i).getString("nnum"));
 					UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));
 					String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode");
 					String cprojectid = list.getJSONObject(i).getString("cprojectid")==null?"":list.getJSONObject(i).getString("cprojectid");  
					String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"":list.getJSONObject(i).getString("clocationCode");  //货位编码
					String Locationid = GetLocationid(cwarehouseid,clocationCode);
					String cstateid = list.getJSONObject(i).getString("cstateid");  //库存状态ID
					String clocationid = list.getJSONObject(i).getString("clocationid") == null ? "":list.getJSONObject(i).getString("clocationid");//货位id
 					
 					for (int j=0;j<bodys.length;j++){
 						String sourcebillbid = bodys[j].getCsourcebillbid()==null?"null":bodys[j].getCsourcebillbid();
 						String cfirstbillbid = bodys[j].getCfirstbillbid()==null?"null":bodys[j].getCfirstbillbid();  //源头单据表体ID
 						String cfirstbillhid = InfoQuery(cfirstbillbid,"cfirstbillhid");  //重新查询获取源头单据表头ID
 						String cgddh = InfoQuery(cfirstbillbid,"cgddh");  //重新查询获取源头单据号
 						String notebody = bodys[j].getVnotebody()==null?"null":bodys[j].getVnotebody();
 						if(pk_arriveorder_b.equals(sourcebillbid)){
 							HashMap<String,Object> poinfo = queryPo(pk_arriveorder_b);//查询可入库数量
							UFDouble krkzsl = poinfo.get("krkzsl")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(poinfo.get("krkzsl")));
							UFDouble krksl = poinfo.get("krksl")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(poinfo.get("krksl")));
 							bodys[j].setNshouldnum(krkzsl);
 							bodys[j].setNnum(nnum);
 							bodys[j].setNshouldassistnum(krksl);
 							bodys[j].setNassistnum(nastnum);
 							bodys[j].setNqtunitnum(nastnum); 
 							bodys[j].setVbatchcode(vbatchcode);
// 							bodys[j].setVbdef3(InfoQuery(bodys[j].getCmaterialoid(),"cwfl"));
 							bodys[j].setPk_batchcode(queryPK_batchcode(vbatchcode,bodys[j].getCmaterialoid()));
 							bodys[j].setPk_creqwareid(cwarehouseid);
 							bodys[j].setCfirstbillhid(cfirstbillhid);
 							bodys[j].setVfirstbillcode(cgddh);
 							UFDouble hsje=bodys[j].getNorigtaxprice().multiply(nnum).setScale(2, UFDouble.ROUND_HALF_UP);  //含税金额
// 							UFDouble bhsje=bodys[j].getNorigprice().multiply(nnum).setScale(2, UFDouble.ROUND_HALF_UP);
 							UFDouble bhsje=hsje.div(UFDouble.ONE_DBL.add(bodys[j].getNtaxrate().multiply(0.01))).setScale(2, UFDouble.ROUND_HALF_UP);   //无税金额    算法改为财务的那种倒推计算法
 							bodys[j].setNorigtaxmny(hsje);
 							bodys[j].setNtaxmny(hsje);
 							bodys[j].setNcaltaxmny(bhsje);
 							bodys[j].setNmny(bhsje);
 							bodys[j].setNorigmny(bhsje);
 							bodys[j].setNtax(hsje.sub(bhsje));
 							bodys[j].setDbizdate(dmakedate_d);
							bodys[j].setDproducedate(dmakedate_d);
							bodys[j].setDvalidate(getDvalidate(bodys[j].getCmaterialoid(),PV[c].getParentVO().getPk_org(),new UFDate(System.currentTimeMillis())));	
							bodys[j].setCprojectid(cprojectid);
							bodys[j].setClocationid(Locationid);
							bodys[j].setClocationid(clocationid);
							bodys[j].setCstateid(cstateid);
// 							bodys[j].setVbdef4(vbdef4);
 							newbodys[j]=bodys[j];	
 							newbodys[j].setVnotebody(notebody=="null"?"":notebody);
 							break;
 						}
 					}
 				}
 				if(newbodys[0]==null){
 					throw new Exception("传输的到货单表体记录都不满足入库条件,请检查！");
 				}
 				PV[c].setChildrenVO(newbodys);
 				result = (PurchaseInVO[])pfaction.processAction("WRITE", "45", null, PV[c], null, null);
 				billcode = result[0].getParentVO().getVbillcode();
 				successMessage = "采购入库单保存";
 				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "45", null, result[0], null, null);
					successMessage = successMessage+"并签字";
				}
 			}
 			
 			returnJson.setResultBillcode(billcode);
 			returnJson.setReturnMessage(successMessage+"成功！");
 			returnJson.setStatus("1");
 		} catch (Exception e) {
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 			if(result!=null&&result.length>0){
 				String[] PurchaseInPKS = new String[]{result[0].getPrimaryKey()};
 				IPurchaseInQueryAPI PQ = (IPurchaseInQueryAPI) NCLocator.getInstance().lookup(IPurchaseInQueryAPI.class);
                 
 				try {  //如果生成了采购入库，而审批异常，则删除采购入库单
 					PurchaseInVO[] newPurchaseInVO= (PurchaseInVO[])PQ.queryVOByIDs(PurchaseInPKS);
 					pfaction.processAction("DELETE", "45", null, newPurchaseInVO[0], null, null);
 				} catch (BusinessException e1) {
 					// TODO 自动生成的 catch 块
 					e1.printStackTrace();
 				}
 			}
 		}
 		return RestUtils.toJSONString(returnJson);
     }
     
 	public JSONString Update45(JSONObject jsonAy) {
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
			UFDate dmakedate_d = dmakedate_t.getDate();	
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			
			String vnote = jsonAy.getString("vnote");  //备注
 			String successMessage = "";;
 			
 			String cgeneralhid = jsonAy.getString("cgeneralhid");  //产成品入库单ID
 			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
 			PurchaseInVO GIVO_ori = IQ.querySingleBillByPk(PurchaseInVO.class, cgeneralhid);
 			if(GIVO_ori==null)
 				throw new Exception("采购入库单ID："+cgeneralhid+"在NC不存在，请检查！");
 			String pk_group = GIVO_ori.getParentVO().getPk_group();
 			
 			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
 			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
// 			InvocationInfoProxy.getInstance().setBizDateTime(audittime.getMillis());
 			int Fbillflag = GIVO_ori.getParentVO().getFbillflag();
 			if(Fbillflag!=2) {
 				PurchaseInVO[] tempvo = (PurchaseInVO[])pfaction.processAction("CANCELSIGN", "45", null, GIVO_ori, null, null);
 				GIVO_ori = tempvo[0];
 			}
 			PurchaseInVO[] GIVO_ori2 = {GIVO_ori};
 			PurchaseInVO GIVO = (PurchaseInVO)GIVO_ori.clone();
 			PurchaseInVO[] GIVO2 = {GIVO};
 			PurchaseInBodyVO[] bvos = GIVO.getBodys();
 			PurchaseInHeadVO hvo = GIVO.getHead();
 			hvo.setVdef2("Y");
 			hvo.setStatus(VOStatus.UPDATED);
 			hvo.setDbilldate(dmakedate_d);
 			JSONArray list = jsonAy.getJSONArray("list");
 			for(int i=0;i<list.size();i++){
 				String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid")==null?"null":list.getJSONObject(i).getString("cgeneralbid");  //表体主键 				
 				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));  //入库主数量
 				UFDouble nassistnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));  //入库数量
 				String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode");
 				String cstateid = list.getJSONObject(i).getString("cstateid")==null?"":list.getJSONObject(i).getString("cstateid");
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
 						bvos[j].setDbizdate(dmakedate_d);
 						if(!unitweight.equals(UFDouble.ZERO_DBL))
 							bvos[j].setNweight(unitweight.multiply(nnum).setScale(4, UFDouble.ROUND_HALF_UP));
 						bvos[j].setStatus(VOStatus.UPDATED);			
 					}
 				}
 			}
 			
// 			FinProdInVO[] result = (FinProdInVO[])pfaction.processAction("WRITE", "46", null, GIVO, null, null);
 			IPurchaseInMaintain PQ = (IPurchaseInMaintain) NCLocator.getInstance().lookup(IPurchaseInMaintain.class);
 			PurchaseInVO[] result = PQ.update(GIVO2,GIVO_ori2);
 			successMessage = "NC采购入库单保存";
 			if(sighFlag.equals("Y")){
 				InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
 				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
 				pfaction.processAction("SIGN", "45", null, result[0], null, null);
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
 	
 	public JSONString Generate4H(JSONObject jsonAy) {

		returnvo returnJson = new returnvo();
		BorrowOutVO[] result = null;
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		Map ret = new HashMap();
		try {
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			BorrowOutVO Bovo = new BorrowOutVO();
			BorrowOutHeadVO hvo = new BorrowOutHeadVO();
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //入库仓库id

			String vnote = jsonAy.getString("vnote");  //表头备注
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		    String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
			String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
			
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
			
			JSONArray list = jsonAy.getJSONArray("list");
			BorrowOutBodyVO[] bvos = new BorrowOutBodyVO[list.size()];
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
			String successMessage = "";
			
			hvo.setBinitial(UFBoolean.FALSE);
			hvo.setCorpoid(pk_org);
			hvo.setCorpvid(pk_org_v);
			hvo.setCtrantypeid("0001A110000000002DYQ");
			hvo.setCwarehouseid(cwarehouseid);
			hvo.setDbilldate(dmakedate_t.getDate());
			hvo.setFbillflag(2);
			hvo.setIprintcount(0);
			hvo.setPk_group(pk_group);
			hvo.setPk_org(pk_org);
			hvo.setPk_org_v(pk_org_v);
			hvo.setVtrantypecode("4H-01");
			hvo.setVnote(vnote);
			hvo.setStatus(VOStatus.NEW);
			
			for(int i=0;i<list.size();i++){
				String crowno = list.getJSONObject(i).getString("crowno");  //行号
				String cmaterialoid = list.getJSONObject(i).getString("cmaterialoid");  //物料id
//				String clocationCode = list.getJSONObject(i).getString("clocationCode");  //货位编码
				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));   //主数量
				UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));	//辅数量		
				String vbatchcode = list.getJSONObject(i).getString("vbatchcode");  //批次号
				String cstateid = list.getJSONObject(i).getString("cstateid");
				String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
						+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
				HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
				String castunitid = "";
				String pk_source = "";
				String zdw = "";
				String measrate = "";
				if(materialMap!=null&&materialMap.get("zdw")!=null){
					castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
					pk_source=materialMap.get("pk_source").toString();
					zdw=materialMap.get("zdw").toString();
					measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
				}else
					throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
				
				HashMap<String,Object> matinfo = queryMaterialBD(cmaterialoid);//查询物料单位重量
				UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight")));
				bvos[i] = new BorrowOutBodyVO();
				bvos[i].setBbarcodeclose(UFBoolean.FALSE);
				bvos[i].setBonroadflag(UFBoolean.FALSE);
				bvos[i].setCastunitid(castunitid);
				bvos[i].setCbodytranstypecode("4H-01");
				bvos[i].setCbodywarehouseid(cwarehouseid);
				bvos[i].setCmaterialoid(cmaterialoid);
				bvos[i].setCmaterialvid(pk_source);
				bvos[i].setCorpoid(pk_org);
				bvos[i].setCorpvid(pk_org_v);
//				bvos[i].setCrowno(String.valueOf(i)+"0");
				bvos[i].setCrowno(crowno);
				bvos[i].setCunitid(zdw);
				bvos[i].setDbizdate(dmakedate_t.getDate());
				bvos[i].setNassistnum(nastnum);
				bvos[i].setNleftastnum(nastnum);
				bvos[i].setNleftnum(nnum);
				bvos[i].setNnum(nnum);
				bvos[i].setNvolume(UFDouble.ZERO_DBL);
				bvos[i].setNweight(UFDouble.ZERO_DBL);
				bvos[i].setPk_group(pk_group);
				bvos[i].setPk_org(pk_org);
				bvos[i].setPk_org_v(pk_org_v);
				bvos[i].setVchangerate(measrate);
				bvos[i].setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
				bvos[i].setVbatchcode(vbatchcode);
				bvos[i].setCstateid(cstateid);
				bvos[i].setDproducedate(dmakedate_t.getDate());
				bvos[i].setDvalidate(getDvalidate(cmaterialoid,pk_org,dmakedate_t.getDate()));
				bvos[i].setStatus(VOStatus.NEW);
			}
			Bovo.setParentVO(hvo);
			Bovo.setChildrenVO(bvos);
			result=(BorrowOutVO[])pfaction.processAction("WRITE", "4H", null, Bovo,null, null);
			successMessage = "NC借出单保存";
			if(sighFlag.equals("Y")){
				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
				pfaction.processAction("SIGN", "4H", null, result[0], null, null);
				successMessage = successMessage+"并签字";
			}
			successMessage = successMessage+"成功！";
			ret.put("ResultBillcode", Bovo.getParentVO().getAttributeValue("vbillcode").toString());
			ret.put("ReturnMessage", successMessage);
			ret.put("Status","1");
			ret.put("cgeneralhid",(String)result[0].getParentVO().getAttributeValue("cgeneralhid"));
			List<Map> mapList2 = new ArrayList();
			BorrowOutBodyVO[] retbvo = result[0].getBodys();
			for(int k=0;k<retbvo.length;k++) {
				String cmaterialoid = bvos[k].getCmaterialoid();
				String cgeneralbid = bvos[k].getCgeneralbid();
				Map map_msv = new HashMap();
				map_msv.put("crowno", bvos[k].getCrowno());
				map_msv.put("cmaterialoid", cmaterialoid);
				map_msv.put("cgeneralbid", cgeneralbid);
				mapList2.add(map_msv);
			}
			ret.put("ids",mapList2);
		} catch (Exception e) {
			ret.put("ResultBillcode", "");
			ret.put("ReturnMessage", e.toString());
			ret.put("Status","0");
			if(result!=null&&result.length>0){
				String pk = result[0].getHead().getCgeneralhid();
				BorrowOutVO errorVo = IQ.querySingleBillByPk(BorrowOutVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4H", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(ret);
 	}
 	
 	public JSONString Generate49(JSONObject jsonAy) {
		returnvo returnJson = new returnvo();
		BorrowInVO[] result = null;
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		Map ret = new HashMap();
		try {
			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
			BorrowInVO Bivo = new BorrowInVO();
			BorrowInHeadVO hvo = new BorrowInHeadVO();
			String cwarehouseid = jsonAy.getString("cwarehouseid");  //入库仓库id

			String vnote = jsonAy.getString("vnote");  //表头备注
			String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			String sql_org = "select s.pk_org from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
		    String sql_group = "select s.pk_group from bd_stordoc s where s.pk_stordoc='"+cwarehouseid+"'";
			String pk_org = (String)dao.executeQuery(sql_org, new ColumnProcessor());
			String pk_group = (String)dao.executeQuery(sql_group, new ColumnProcessor());
			String sql_org_v = "select o.pk_vid from org_orgs o where o.pk_org='"+pk_org+"'";
			String pk_org_v = (String)dao.executeQuery(sql_org_v, new ColumnProcessor());
			
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
			
			JSONArray list = jsonAy.getJSONArray("list");
			BorrowInBodyVO[] bvos = new BorrowInBodyVO[list.size()];
			InvocationInfoProxy.getInstance().setGroupId(pk_group);  //设置集团环境变量
			InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量，用户为WMS
			String successMessage = "";
			
			hvo.setCorpoid(pk_org);
			hvo.setCorpvid(pk_org_v);
			hvo.setCtrantypeid("0001A110000000002DY3");
			hvo.setCwarehouseid(cwarehouseid);
			hvo.setDbilldate(dmakedate_t.getDate());
			hvo.setFbillflag(2);
			hvo.setIprintcount(0);
			hvo.setPk_group(pk_group);
			hvo.setPk_org(pk_org);
			hvo.setPk_org_v(pk_org_v);
			hvo.setVtrantypecode("49-01");
			hvo.setVnote(vnote);
			hvo.setStatus(VOStatus.NEW);
			
			for(int i=0;i<list.size();i++){
				String crowno = list.getJSONObject(i).getString("crowno");  //行号
				String cmaterialoid = list.getJSONObject(i).getString("cmaterialoid");  //物料id
//				String clocationCode = list.getJSONObject(i).getString("clocationCode");  //货位编码
				UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nnum"));   //主数量
				UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));	//辅数量		
				String vbatchcode = list.getJSONObject(i).getString("vbatchcode");  //批次号
				String cstateid = list.getJSONObject(i).getString("cstateid");
				String sql_astunit = "select mc.pk_measdoc,m.pk_measdoc zdw,v.pk_source,mc.measrate from bd_material m left join bd_materialconvert mc on m.pk_material=mc.pk_material and mc.isstockmeasdoc='Y' and mc.dr=0 "
						+ "left join bd_material_v v on m.pk_material=v.pk_material where m.pk_material='"+cmaterialoid+"'";
				HashMap materialMap = (HashMap) dao.executeQuery(sql_astunit,new MapProcessor());
				String castunitid = "";
				String pk_source = "";
				String zdw = "";
				String measrate = "";
				if(materialMap!=null&&materialMap.get("zdw")!=null){
					castunitid=materialMap.get("pk_measdoc")==null?materialMap.get("zdw").toString():materialMap.get("pk_measdoc").toString();
					pk_source=materialMap.get("pk_source").toString();
					zdw=materialMap.get("zdw").toString();
					measrate=materialMap.get("measrate")==null?"1.000000/1.000000":materialMap.get("measrate").toString();
				}else
					throw new Exception("物料主键"+cmaterialoid+"在NC系统中不存在！");
				
				HashMap<String,Object> matinfo = queryMaterialBD(cmaterialoid);//查询物料单位重量
				UFDouble unitweight = matinfo.get("unitweight")==null?UFDouble.ZERO_DBL:new UFDouble(String.valueOf(matinfo.get("unitweight")));
				bvos[i] = new BorrowInBodyVO();
				bvos[i].setBbarcodeclose(UFBoolean.FALSE);
				bvos[i].setBcseal(UFBoolean.FALSE);
				bvos[i].setCastunitid(castunitid);
				bvos[i].setCbodytranstypecode("49-01");
				bvos[i].setCbodywarehouseid(cwarehouseid);
				bvos[i].setCmaterialoid(cmaterialoid);
				bvos[i].setCmaterialvid(pk_source);
				bvos[i].setCorpoid(pk_org);
				bvos[i].setCorpvid(pk_org_v);
				bvos[i].setCrowno(crowno);
//				bvos[i].setCrowno(String.valueOf(i)+"0");
				bvos[i].setCunitid(zdw);
				bvos[i].setDbizdate(dmakedate_t.getDate());
				bvos[i].setNassistnum(nastnum);
//				bvos[i].setNleftastnum(nastnum);
//				bvos[i].setNleftnum(nnum);
				bvos[i].setNnum(nnum);
				bvos[i].setNvolume(UFDouble.ZERO_DBL);
				bvos[i].setNweight(UFDouble.ZERO_DBL);
				bvos[i].setPk_group(pk_group);
				bvos[i].setPk_org(pk_org);
				bvos[i].setPk_org_v(pk_org_v);
				bvos[i].setVchangerate(measrate);
				bvos[i].setPk_batchcode(queryPK_batchcode(vbatchcode,cmaterialoid));
				bvos[i].setVbatchcode(vbatchcode);
				bvos[i].setCstateid(cstateid);
				bvos[i].setDproducedate(dmakedate_t.getDate());
				bvos[i].setDvalidate(getDvalidate(cmaterialoid,pk_org,dmakedate_t.getDate()));
				bvos[i].setStatus(VOStatus.NEW);
			}
			Bivo.setParentVO(hvo);
			Bivo.setChildrenVO(bvos);
			result=(BorrowInVO[])pfaction.processAction("WRITE", "49", null, Bivo,null, null);
			successMessage = "NC借入单保存";
			if(sighFlag.equals("Y")){
				String taudittime = jsonAy.getString("taudittime");  //签字日期
				UFDateTime taudittime_t = new UFDateTime(taudittime);			
				InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
				InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
				pfaction.processAction("SIGN", "49", null, result[0], null, null);
				successMessage = successMessage+"并签字";
			}
			successMessage = successMessage+"成功！";
			ret.put("ResultBillcode", Bivo.getParentVO().getAttributeValue("vbillcode").toString());
			ret.put("ReturnMessage", successMessage);
			ret.put("Status","1");
			ret.put("cgeneralhid",(String)result[0].getParentVO().getAttributeValue("cgeneralhid"));
			List<Map> mapList2 = new ArrayList();
			BorrowInBodyVO[] retbvo = result[0].getBodys();
			for(int k=0;k<retbvo.length;k++) {
				String cmaterialoid = bvos[k].getCmaterialoid();
				String cgeneralbid = bvos[k].getCgeneralbid();
				Map map_msv = new HashMap();
				map_msv.put("crowno", bvos[k].getCrowno());
				map_msv.put("cmaterialoid", cmaterialoid);
				map_msv.put("cgeneralbid", cgeneralbid);
				mapList2.add(map_msv);
			}
			ret.put("ids",mapList2);
		} catch (Exception e) {
			ret.put("ResultBillcode", "");
			ret.put("ReturnMessage", e.toString());
			ret.put("Status","0");
			if(result!=null&&result.length>0){
				String pk = result[0].getHead().getCgeneralhid();
				BorrowInVO errorVo = IQ.querySingleBillByPk(BorrowInVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "49", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
		return RestUtils.toJSONString(ret);
 	}
 	
 	public JSONString Generate4B(JSONObject jsonAy) {
 		returnvo returnJson = new returnvo();
 		ReturnInVO[] result = null;
 		String billcode = "";
 		try {
 			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
 			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
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
			 String vnote = jsonAy.getString("vnote");  //备注
			 String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			 String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			 String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
			 UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			 UFDate dmakedate_d = dmakedate_t.getDate();	
			 InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			 JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
			 InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
			 InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
			 String successMessage = "";
 			
 			String cgeneralhid = jsonAy.getString("cgeneralhid");  //借出单表头ID
 			BorrowOutVO bo_data = IQ.querySingleBillByPk(BorrowOutVO.class, cgeneralhid);
 			if (bo_data==null){
 				throw new Exception("传输的借出单ID在系统不存在或者已删除!");
 			}
 			BorrowOutVO[] bo_data2 = {bo_data};
 			ReturnInVO[] PV = (ReturnInVO[])PfUtilTools.runChangeDataAry("4H","4B", bo_data2);
 			for (int c=0;c<PV.length;c++){
 				PV[c].getParentVO().setCwarehouseid(cwarehouseid);
 				PV[c].getParentVO().setVnote(vnote);
 				PV[c].getParentVO().setCtrantypeid("0001A110000000002DY4");
				PV[c].getParentVO().setVtrantypecode("4B-01");
				PV[c].getParentVO().setStatus(VOStatus.NEW);
				ReturnInBodyVO[] bodys = (ReturnInBodyVO[])PV[c].getBodys();
// 				PurchaseInBodyVO[] newbodys = new PurchaseInBodyVO[bodys.length];  //重新组建表体VO
				ArrayList<ReturnInBodyVO> bodylist = new ArrayList<ReturnInBodyVO>();
 				for (int i=0;i<list.size();i++){
 					String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid")==null?"null":list.getJSONObject(i).getString("cgeneralbid");
 					UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(i).getString("nnum"));
 					UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));
 					String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode"); 
					String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"":list.getJSONObject(i).getString("clocationCode");  //货位编码
					String Locationid = GetLocationid(cwarehouseid,clocationCode);
					String cstateid = list.getJSONObject(i).getString("cstateid");  //库存状态ID
 					
 					for (int j=0;j<bodys.length;j++){
 						String sourcebillbid = bodys[j].getCsourcebillbid()==null?"null":bodys[j].getCsourcebillbid();
 						String cfirstbillbid = bodys[j].getCfirstbillbid()==null?"null":bodys[j].getCfirstbillbid();  //源头单据表体ID
 						String cfirstbillhid = InfoQuery(cfirstbillbid,"cfirstbillhid");  //重新查询获取源头单据表头ID
 						String cgddh = InfoQuery(cfirstbillbid,"cgddh");  //重新查询获取源头单据号
 						String notebody = bodys[j].getVnotebody()==null?"null":bodys[j].getVnotebody();
 						if(cgeneralbid.equals(sourcebillbid)){
 							ReturnInBodyVO newbody = (ReturnInBodyVO)bodys[j].clone();
 							newbody.setCrowno(String.valueOf(i)+"0");
 							newbody.setNshouldnum(nnum);
 							newbody.setNnum(nnum);
 							newbody.setNshouldassistnum(nastnum);
 							newbody.setNassistnum(nastnum);
 							newbody.setVbatchcode(vbatchcode);
 							newbody.setPk_batchcode(queryPK_batchcode(vbatchcode,newbody.getCmaterialoid()));
 							newbody.setDbizdate(dmakedate_d);
 							newbody.setDproducedate(dmakedate_d);
 							newbody.setDvalidate(getDvalidate(bodys[j].getCmaterialoid(),PV[c].getParentVO().getPk_org(),new UFDate(System.currentTimeMillis())));	
 							newbody.setClocationid(Locationid);
 							newbody.setCstateid(cstateid);
 							newbody.setStatus(VOStatus.NEW);
 							bodylist.add(newbody);
 							break;
 						}
 					}
 				}
 				if(bodylist==null||bodylist.size()==0){
 					throw new Exception("传输的借出单表体记录在NC都不存在，请检查！");
 				}
 				PV[c].setChildrenVO(bodylist.toArray(new ReturnInBodyVO[bodylist.size()]));
 				result = (ReturnInVO[])pfaction.processAction("WRITE", "4B", null, PV[c], null, null);
 				billcode = result[0].getParentVO().getVbillcode();
 				successMessage = "借出还回单保存";
 				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "4B", null, result[0], null, null);
					successMessage = successMessage+"并签字";
				}
 			}
 			
 			returnJson.setResultBillcode(billcode);
 			returnJson.setReturnMessage(successMessage+"成功！");
 			returnJson.setStatus("1");
 		} catch (Exception e) {
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 			if(result!=null&&result.length>0){
				String pk = result[0].getHead().getCgeneralhid();
				ReturnInVO errorVo = IQ.querySingleBillByPk(ReturnInVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4B", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
 		}
 		return RestUtils.toJSONString(returnJson);
 	}
 	
 	public JSONString Generate4J(JSONObject jsonAy) {
 		returnvo returnJson = new returnvo();
 		ReturnOutVO[] result = null;
 		String billcode = "";
 		try {
 			NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes()); 
 			InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
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
			 String vnote = jsonAy.getString("vnote");  //备注
			 String sighFlag = jsonAy.getString("sighFlag");  //是否签字标识
			 String dmakedate = jsonAy.getString("dmakedate");  //制单日期
			 String cwarehouseid = jsonAy.getString("cwarehouseid");  //仓库ID
			 UFDateTime dmakedate_t = new UFDateTime(dmakedate);		
			 UFDate dmakedate_d = dmakedate_t.getDate();	
			 InvocationInfoProxy.getInstance().setBizDateTime(dmakedate_t.getMillis());
			 JSONArray list = jsonAy.getJSONArray("list");  //获取表体记录
			 InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
			 InvocationInfoProxy.getInstance().setUserId(cuserid);    //设置用户环境变量
			 String successMessage = "";
 			
 			String cgeneralhid = jsonAy.getString("cgeneralhid");  //借出单表头ID
 			BorrowInVO bo_data = IQ.querySingleBillByPk(BorrowInVO.class, cgeneralhid);
 			if (bo_data==null){
 				throw new Exception("传输的借入单ID在系统不存在或者已删除!");
 			}
 			BorrowInVO[] bo_data2 = {bo_data};
 			ReturnOutVO[] PV = (ReturnOutVO[])PfUtilTools.runChangeDataAry("49","4J", bo_data2);
 			for (int c=0;c<PV.length;c++){
 				PV[c].getParentVO().setCwarehouseid(cwarehouseid);
 				PV[c].getParentVO().setVnote(vnote);
 				PV[c].getParentVO().setCtrantypeid("0001A110000000002DYR");
				PV[c].getParentVO().setVtrantypecode("4J-01");
				PV[c].getParentVO().setStatus(VOStatus.NEW);
				ReturnOutBodyVO[] bodys = (ReturnOutBodyVO[])PV[c].getBodys();
// 				PurchaseInBodyVO[] newbodys = new PurchaseInBodyVO[bodys.length];  //重新组建表体VO
				ArrayList<ReturnOutBodyVO> bodylist = new ArrayList<ReturnOutBodyVO>();
 				for (int i=0;i<list.size();i++){
 					String cgeneralbid = list.getJSONObject(i).getString("cgeneralbid")==null?"null":list.getJSONObject(i).getString("cgeneralbid");
 					UFDouble nnum = list.getJSONObject(i).getString("nnum")==null?UFDouble.ZERO_DBL:new UFDouble(list.getJSONObject(i).getString("nnum"));
 					UFDouble nastnum = list.getJSONObject(i).getString("nastnum")==null?null:new UFDouble(list.getJSONObject(i).getString("nastnum"));
 					String vbatchcode = list.getJSONObject(i).getString("vbatchcode")==null?"":list.getJSONObject(i).getString("vbatchcode"); 
					String clocationCode = list.getJSONObject(i).getString("clocationCode")==null?"":list.getJSONObject(i).getString("clocationCode");  //货位编码
					String Locationid = GetLocationid(cwarehouseid,clocationCode);
					String cstateid = list.getJSONObject(i).getString("cstateid");  //库存状态ID
 					
 					for (int j=0;j<bodys.length;j++){
 						String sourcebillbid = bodys[j].getCsourcebillbid()==null?"null":bodys[j].getCsourcebillbid();
 						String cfirstbillbid = bodys[j].getCfirstbillbid()==null?"null":bodys[j].getCfirstbillbid();  //源头单据表体ID
 						String cfirstbillhid = InfoQuery(cfirstbillbid,"cfirstbillhid");  //重新查询获取源头单据表头ID
 						String cgddh = InfoQuery(cfirstbillbid,"cgddh");  //重新查询获取源头单据号
 						String notebody = bodys[j].getVnotebody()==null?"null":bodys[j].getVnotebody();
 						if(cgeneralbid.equals(sourcebillbid)){
 							ReturnOutBodyVO newbody = (ReturnOutBodyVO)bodys[j].clone();
 							newbody.setCrowno(String.valueOf(i)+"0");
 							newbody.setNshouldnum(nnum);
 							newbody.setNnum(nnum);
 							newbody.setNshouldassistnum(nastnum);
 							newbody.setNassistnum(nastnum);
 							newbody.setVbatchcode(vbatchcode);
 							newbody.setPk_batchcode(queryPK_batchcode(vbatchcode,newbody.getCmaterialoid()));
 							newbody.setDbizdate(dmakedate_d);
 							newbody.setDproducedate(dmakedate_d);
 							newbody.setDvalidate(getDvalidate(bodys[j].getCmaterialoid(),PV[c].getParentVO().getPk_org(),new UFDate(System.currentTimeMillis())));	
 							newbody.setClocationid(Locationid);
 							newbody.setCstateid(cstateid);
 							newbody.setStatus(VOStatus.NEW);
 							bodylist.add(newbody);
 							break;
 						}
 					}
 				}
 				if(bodylist==null||bodylist.size()==0){
 					throw new Exception("传输的借入单表体记录在NC都不存在，请检查！");
 				}
 				PV[c].setChildrenVO(bodylist.toArray(new ReturnOutBodyVO[bodylist.size()]));
 				result = (ReturnOutVO[])pfaction.processAction("WRITE", "4J", null, PV[c], null, null);
 				billcode = result[0].getParentVO().getVbillcode();
 				successMessage = "借入还回单保存";
 				if(sighFlag.equals("Y")){
					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					String taudittime = jsonAy.getString("taudittime");  //签字日期
					UFDateTime taudittime_t = new UFDateTime(taudittime);			
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					InvocationInfoProxy.getInstance().setUserId(approverid);    //设置用户环境变量
					pfaction.processAction("SIGN", "4J", null, result[0], null, null);
					successMessage = successMessage+"并签字";
				}
 			}
 			
 			returnJson.setResultBillcode(billcode);
 			returnJson.setReturnMessage(successMessage+"成功！");
 			returnJson.setStatus("1");
 		} catch (Exception e) {
 			returnJson.setResultBillcode("");
 			returnJson.setStatus("0");
 			returnJson.setReturnMessage(e.toString());
 			if(result!=null&&result.length>0){
				String pk = result[0].getHead().getCgeneralhid();
				ReturnOutVO errorVo = IQ.querySingleBillByPk(ReturnOutVO.class, pk);
				try {  //回滚生成的单据
//					InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
					pfaction.processAction("DELETE", "4J", null, errorVo, null, null);
				} catch (BusinessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
 		}
 		return RestUtils.toJSONString(returnJson);
 	}
 	
}
