package nc.pubitf.ic.wms.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import nc.vo.scmpub.api.rest.utils.RestUtils;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.framework.server.ISecurityTokenCallback;
import nc.bs.ic.pub.env.ICBSContext;
import nc.bs.pub.pf.PfUtilTools;
import nc.impl.ic.util.DateCalUtil;
import nc.impl.obm.pattern.data.bill.BillQuery;
import nc.itf.ic.m4c.self.ISaleOutMaintain;
import nc.itf.ic.m4n.ITransformMaitain;
import nc.itf.ic.m4r.IInvCountAdjust;
import nc.itf.ic.m4r.IInvCountMaintain;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.uap.pf.IPFBusiAction;
import nc.itf.uap.pf.IPfExchangeService;
import nc.jdbc.framework.generator.IdGenerator;
import nc.jdbc.framework.generator.SequenceGenerator;
import nc.jdbc.framework.processor.BeanListProcessor;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.pubitf.ic.m4a.api.IGeneralInMaintainAPI;
import nc.pubitf.ic.m4c.api.ISaleOutMaintainAPI;
import nc.pubitf.ic.m4i.api.IGeneralOutMaintainAPI;
import nc.pubitf.scmf.ic.mbatchcode.mmpac.IBatchCodeQuery;
import nc.pubitf.so.m4331.api.IDeliveryQueryAPI;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.general.define.ICBillVO;
import nc.vo.ic.m4a.entity.GeneralInBodyVO;
import nc.vo.ic.m4a.entity.GeneralInVO;
import nc.vo.ic.m4c.entity.SaleOutBodyVO;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.ic.m4d.entity.MaterialOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4n.entity.TransformBodyVO;
import nc.vo.ic.m4n.entity.TransformHeadVO;
import nc.vo.ic.m4n.entity.TransformVO;
import nc.vo.ic.m4r.entity.InvCountBillVO;
import nc.vo.ic.m4r.entity.InvCountBodyVO;
import nc.vo.ic.material.define.InvCalBodyVO;
import nc.vo.ic.pub.util.NCBaseTypeUtils;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.calculator.HslParseUtil;
import nc.vo.scmf.ic.mbatchcode.BatchcodeVO;
import nc.vo.so.m4331.entity.DeliveryBVO;
import nc.vo.so.m4331.entity.DeliveryVO;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONString;

public class WmsSaleoutNCRestImpl {

	public static String getValue(String key) {
		Properties proper = null;
		if (proper == null) {
			try {
				proper = new Properties();
				proper.load(WmsSaleoutNCRestImpl.class.getClassLoader().getResourceAsStream("Wmsconfig.properties"));
			} catch (Exception e) {
				e.printStackTrace();
				proper = null;
				return null;
			}
		}
		return proper.getProperty(key);
	}

	public JSONString wmsDeliveryTo4C(JSONObject  str){

		JSONObject retjson = new JSONObject();
		retjson.put("Status", "0");//默认失败

		BaseDAO dao = new BaseDAO();
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
		String psnid = str.getString("userID");
		String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+psnid+"'";
		String userID = "";
		try {
			userID = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
		} catch (DAOException e2) {
			retjson.put("ReturnMessage", "查询用户ID报错："+e2.getMessage());
			return RestUtils.toJSONString(retjson);
		}

		String approverPsnID = str.getString("approverID");
		String sql_approverid = "select u.cuserid from sm_user u where u.pk_psndoc='"+approverPsnID+"'";
		String approverID = "";
		try {
			approverID = (String) dao.executeQuery(sql_approverid, new ColumnProcessor());
		} catch (DAOException e2) {
			retjson.put("ReturnMessage", "查询用户ID报错："+e2.getMessage());
			return RestUtils.toJSONString(retjson);
		}

		if(StringUtils.isEmpty(userID) || StringUtils.isEmpty(approverID) ) {
			retjson.put("ReturnMessage", "根据人员PK查询用户PK为空，请检查[制单人、审批人]");
			return RestUtils.toJSONString(retjson);
		}

		InvocationInfoProxy.getInstance().setUserId(userID);

		SaleOutVO[] saveVOS;
		String deliveryhid = str.getString("cbillid");//发货单ID


		IDeliveryQueryAPI deliveryService = NCLocator.getInstance().lookup( IDeliveryQueryAPI.class);//发货单API
		DeliveryVO[] srcvos = null;
		try {
			srcvos = deliveryService.queryVOByIDs(new String[] {deliveryhid});
		} catch (BusinessException e1) {
			retjson.put("ReturnMessage", "查询发货单报错："+e1.getMessage());
			return RestUtils.toJSONString(retjson);
		}
		if(srcvos == null || srcvos.length == 0) {
			retjson.put("ReturnMessage", "发货单不存在！");
			return RestUtils.toJSONString(retjson);
		}
		DeliveryBVO[] srcbvos = srcvos[0].getChildrenVO();
		List<String> srcbids = new ArrayList();
		for(DeliveryBVO srcbvo : srcbvos) {
			srcbids.add(srcbvo.getCdeliverybid());
		}
		String vnote = str.getString("vnote");
		String cwarehouseid = str.getString("cwarehouseid");
		String dmakedate = str.getString("dmakedate");
		String taudittime = str.getString("taudittime");
		UFDateTime ddmakedate = new UFDateTime(dmakedate);
		UFDateTime dtaudittime = new UFDateTime(taudittime);


		InvocationInfoProxy.getInstance().setBizDateTime(ddmakedate.getMillis());

		List<DeliveryBVO> wmsBodys = new ArrayList();
		List<String> bids = new ArrayList<String>();
		JSONArray bodylists = str.getJSONArray("list");
		for (int i = 0; i < bodylists.size(); i++) {
			JSONObject wmsbvo = bodylists.getJSONObject(i);
			String wmsbid = wmsbvo.getString("cbill_bid");
			if(!srcbids.contains(wmsbid)) {
				retjson.put("ReturnMessage", "发货单["+srcvos[0].getParentVO().getVbillcode()+"]中不存在行：["+wmsbid+"]！");
				return RestUtils.toJSONString(retjson);
			}else {
				for(DeliveryBVO srcbvo : srcbvos) {
					if(wmsbid.equals(srcbvo.getCdeliverybid()) && (!bids.contains(srcbvo.getCdeliverybid()))) {
						wmsBodys.add(srcbvo);
						bids.add(srcbvo.getCdeliverybid());
						break;
					}
				}
			}
		}
		srcvos[0].setChildren(DeliveryBVO.class, wmsBodys.toArray(new DeliveryBVO[0]));

		//调用单据转换规则生成出库单
		AggregatedValueObject[] destVos = null;
		try {
			destVos = getDestAggVO("4331","4C",srcvos);
		} catch (BusinessException e1) {
			retjson.put("ReturnMessage", "执行单据转换规则报错："+e1.getMessage());
			return RestUtils.toJSONString(retjson);
		}
		List<SaleOutVO> saleoutVOS = new ArrayList();
		for(AggregatedValueObject destAggVo : destVos){
			SaleOutVO destVo = (SaleOutVO)destAggVo;
			destVo.getParentVO().setCwarehouseid(cwarehouseid);
			destVo.getParentVO().setDmakedate(ddmakedate.getDate());
			destVo.getParentVO().setCreator(userID);
			destVo.getParentVO().setBillmaker(userID);
			destVo.getParentVO().setVnote(vnote);
			destVo.getParentVO().setFbillflag(2);
			destVo.getParentVO().setStatus(VOStatus.NEW);
			if(StringUtils.isEmpty(destVo.getParentVO().getCtrantypeid())) {
				destVo.getParentVO().setCtrantypeid("1001A21000000007BTV4");
				destVo.getHead().setVtrantypecode("4C-Cxx-04");
			}

			List<SaleOutBodyVO> newbodys = new ArrayList<>();//拆行新增的表体行
			Map<String, UFDouble> rownos = new HashMap<String, UFDouble>();
			for (int j = 0; j < bodylists.size(); j++) {//处理转换后销售出库的字段
				JSONObject wmsbvo = bodylists.getJSONObject(j);
				String wmsbid = wmsbvo.getString("cbill_bid");
				String nnum = wmsbvo.getString("nnum");
				String nastnum = wmsbvo.getString("nastnum");
				String vbatchcode = wmsbvo.getString("vbatchcode");
				String vnotebody = wmsbvo.getString("vnotebody");

				String clocationCode  = wmsbvo.getString("clocationCode");
				String cstateid  = wmsbvo.getString("cstateid");
				String vbdef9  = wmsbvo.getString("vbdef9");
				String vbdef12  = wmsbvo.getString("vbdef12");
				String vbdef13  = wmsbvo.getString("vbdef13");
				String vbdef8 = wmsbvo.getString("vbdef8");


				for (int i = 0; i < destVo.getChildrenVO().length; i++) {
					SaleOutBodyVO bvo = (SaleOutBodyVO) destVo.getBody(i).clone();
					if(bvo.getCsourcebillbid().equals(wmsbid)){
						if(rownos.containsKey(wmsbid)) {
							bvo.setNshouldnum(null);
							bvo.setNshouldassistnum(null);
							String crowno = (rownos.get(wmsbid).add(new UFDouble(0.1))).setScale(2, UFDouble.ROUND_HALF_UP).toString();
							bvo.setCrowno(crowno);
						}

						bvo.setNnum(new UFDouble(nnum));
						bvo.setNassistnum(new UFDouble(nastnum));
//							destVo.getChildrenVO()[i].setVnotebody(clocationCode);//货位不知道放哪
						bvo.setClocationid(clocationCode);
						bvo.setCbodywarehouseid(cwarehouseid);
						UFDouble fsl = bvo.getNassistnum();
						UFDouble ntaxrate = new UFDouble(1).add(bvo.getNtaxrate().div(new UFDouble(100)));
						String sql_batch = "select PK_BATCHCODE from scm_batchcode where cmaterialoid = '"+bvo.getCmaterialoid()+"' and VBATCHCODE = '"+vbatchcode+"' and dr = '0'";
						String pk_batchcode = "";
						try {
							pk_batchcode = (String) dao.executeQuery(sql_batch, new ColumnProcessor());
							bvo.setVbatchcode(vbatchcode);
							bvo.setPk_batchcode(pk_batchcode);
						} catch (DAOException e2) {
							retjson.put("ReturnMessage", "查询批次号ID报错："+e2.getMessage());
							return RestUtils.toJSONString(retjson);
						}
						if(StringUtils.isEmpty(pk_batchcode)) {
							bvo.setVbatchcode(null);
							bvo.setPk_batchcode(null);
						}
						String dproducedateSQL = "SELECT dproducedate FROM scm_batchcode WHERE PK_BATCHCODE = '"+pk_batchcode+"'";
						String dvalidateSQL = "SELECT dvalidate FROM scm_batchcode WHERE PK_BATCHCODE = '"+pk_batchcode+"'";
						try {
							String dproducedate =  (String) dao.executeQuery(dproducedateSQL, new ColumnProcessor());
							String dvalidate =  (String) dao.executeQuery(dvalidateSQL, new ColumnProcessor());
							bvo.setDproducedate(new UFDate(dproducedate));
							bvo.setDvalidate(new UFDate(dvalidate));
						} catch (Exception e) {
							// TODO: handle exception
						}

						bvo.setCstateid(cstateid);
//							bvo.setCrowno(null);
						bvo.setStatus(VOStatus.NEW);

						// 修复价格是外币时，转换价格出错问题
						bvo.setNorigtaxmny(fsl.multiply(bvo.getNqtorigtaxnetprice()).setScale(2, UFDouble.ROUND_HALF_UP));//价税合计 = 含税净价 * 辅数量
						bvo.setNcaltaxmny(bvo.getNorigtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//计税金额
						bvo.setNorigmny(bvo.getNorigtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//无税金额
						bvo.setNtaxmny(fsl.multiply(bvo.getNqttaxnetprice()).setScale(2, UFDouble.ROUND_HALF_UP));//本币价税合计
						bvo.setNmny(bvo.getNtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//本币无税金额
						bvo.setNqtunitnum(fsl);
						bvo.setNtax(bvo.getNtaxmny().sub(bvo.getNmny()));
						bvo.setVnotebody(vnotebody);
						bvo.setVbdef9(vbdef9);
						bvo.setVbdef12(vbdef12);
						bvo.setVbdef13(vbdef13);
						bvo.setVbdef8(vbdef8);
						rownos.put(wmsbid, new UFDouble(bvo.getCrowno()));
						newbodys.add(bvo);
						break;
					}
				}
			}
			destVo.setChildrenVO(newbodys.toArray(new SaleOutBodyVO[0]));
			saleoutVOS.add(destVo);
		}

		ISaleOutMaintain ISaleOutMaintain = NCLocator.getInstance().lookup( ISaleOutMaintain.class);//出库单API
		ISaleOutMaintainAPI saleoutApi = NCLocator.getInstance().lookup(ISaleOutMaintainAPI.class);

		try{
// 			防止单据转换规则出现交易类型为空
//			if (destVo.getHead().getCtrantypeid() == null) {
//				String transtype = "45-01";
//				String transtypePk = "0001N710000000001BOB";
//				destVo.getHead().setCtrantypeid(transtypePk);
//				destVo.getHead().setVtrantypecode(transtype);
//			}
			InvocationInfoProxy.getInstance().setUserId(userID);//设置系统操作人
			saveVOS = ISaleOutMaintain.insert(saleoutVOS.toArray(new SaleOutVO[0]));//保存
			if(saveVOS != null && saveVOS.length > 0) {
				InvocationInfoProxy.getInstance().setUserId(approverID);//设置系统操作人
				InvocationInfoProxy.getInstance().setBizDateTime(dtaudittime.getMillis());
				saveVOS[0].getParentVO().setTaudittime(dtaudittime.getDate());
				saveVOS[0].getParentVO().setApprover(approverID);
				saveVOS = saleoutApi.signBills(saveVOS);//签字
			}
		} catch (Exception e) {
			e.printStackTrace();
			retjson.put("ReturnMessage", "销售出库单保存并签字报错："+e);
			return RestUtils.toJSONString(retjson);
		}

		List<String> vbillCodes = new ArrayList<>();
		if(saveVOS != null && saveVOS.length > 0) {
			for(SaleOutVO vo : saveVOS) {
				vbillCodes.add(vo.getHead().getVbillcode());
			}
			retjson.put("Status", "1");
			retjson.put("ResultBillcode", vbillCodes.get(0));
			retjson.put("ReturnMessage", "NC销售出库保存并签字成功！");
		}


		return RestUtils.toJSONString(retjson);

	}

	/**
	 * 调用单据转换规则 sourcetype：源头单据类型 desttype：目标单据类型 vo：转换实体
	 *
	 * @return
	 */
	protected AggregatedValueObject[] getDestAggVO(String sourcetype, String desttype, AggregatedValueObject[] vos)
			throws BusinessException {
		AggregatedValueObject[] destVOs = PfUtilTools.runChangeDataAry(sourcetype, desttype, vos);
		return destVOs;
	}


	public JSONString wmsSaleoutTo4C(JSONObject  str)  {

		JSONObject retjson = new JSONObject();
		retjson.put("Status", "0");//默认失败

		BaseDAO dao = new BaseDAO();
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
		String psnid = str.getString("approverID");
		String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+psnid+"'";
		String approverID = "";
		try {
			approverID = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
		} catch (DAOException e2) {
			retjson.put("ReturnMessage", "查询用户ID报错："+e2.getMessage());
			return RestUtils.toJSONString(retjson);
		}

		if(StringUtils.isEmpty(approverID) ) {
			retjson.put("ReturnMessage", "根据人员PK查询用户PK为空，请检查[审批人]");
			return RestUtils.toJSONString(retjson);
		}

		InvocationInfoProxy.getInstance().setUserId(approverID);

		SaleOutVO[] saveVOs;
		SaleOutVO destVo = new SaleOutVO();
		String billid = str.getString("cbillid");//出库表头ID

		ISaleOutMaintain ISaleOutMaintain = NCLocator.getInstance().lookup( ISaleOutMaintain.class);//出库单API
		SaleOutVO[] aggsovo = null;
		BillQuery<SaleOutVO> query = new BillQuery<SaleOutVO>(
				SaleOutVO.class);
		aggsovo = query.query(new String[] {billid});
		if(aggsovo == null || aggsovo.length == 0) {
			retjson.put("ReturnMessage", "出库单不存在！");
			return RestUtils.toJSONString(retjson);
		}else {
			destVo = aggsovo[0];
		}
		SaleOutVO oldvo = (SaleOutVO) destVo.clone();


		String cwarehouseid = str.getString("cwarehouseid");
		String taudittime = str.getString("taudittime");
//		String approverID = str.getString("approverID");

		JSONArray bodylists = str.getJSONArray("list");

		//调用单据转换规则生成出库单
		destVo.getParentVO().setCwarehouseid(cwarehouseid);
//			destVo.getParentVO().setTaudittime(new UFDate(taudittime));
		destVo.getParentVO().setApprover(approverID);
		destVo.getParentVO().setStatus(VOStatus.UPDATED);
		Map<String,String> rows_map = new HashMap<String,String>();  //用来记录是否多次传同个ID,以便判断是修改还是增行
		List<SaleOutBodyVO> bodylist = new ArrayList();
		for (int j = 0; j < bodylists.size(); j++) {
			JSONObject wmsbvo = bodylists.getJSONObject(j);
			String wmsbid = wmsbvo.getString("cbill_bid");
			String nnum = wmsbvo.getString("nnum");
			String nastnum = wmsbvo.getString("nastnum");
			String vbatchcode = wmsbvo.getString("vbatchcode");
			String clocationCode  = wmsbvo.getString("clocationCode");
			String cstateid  = wmsbvo.getString("cstateid");
			String vnotebody  = wmsbvo.getString("vnotebody");
			SaleOutBodyVO[] bvos = (SaleOutBodyVO[])destVo.getChildrenVO();
			int rowcount = bvos.length;
			for (int i = 0; i < bvos.length; i++) {
				SaleOutBodyVO bvo = bvos[i];
				if(bvo.getCsourcebillbid().equals(wmsbid) || bvo.getCgeneralbid().equals(wmsbid)){
					SaleOutBodyVO tabvo = null;
					String hadsendflag = (String)rows_map.get(wmsbid);
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
							tabvo = (SaleOutBodyVO)bvos[j].clone();
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
						rows_map.put(wmsbid, wmsbid);
					}

					tabvo.setNnum(new UFDouble(nnum));
					tabvo.setNassistnum(new UFDouble(nastnum));
					tabvo.setCbodywarehouseid(cwarehouseid);

					UFDouble fsl = tabvo.getNassistnum();
					UFDouble ntaxrate = new UFDouble(1).add(tabvo.getNtaxrate().div(new UFDouble(100)));
					String sql_batch = "select PK_BATCHCODE from scm_batchcode where cmaterialvid = '"+bvo.getCmaterialoid()+"' and VBATCHCODE = '"+vbatchcode+"' and dr = '0'";
					String pk_batchcode = "";
					try {
						pk_batchcode = (String) dao.executeQuery(sql_batch, new ColumnProcessor());
					} catch (DAOException e2) {
						retjson.put("ReturnMessage", "查询批次号ID报错："+e2.getMessage());
						return RestUtils.toJSONString(retjson);
					}
					if(StringUtils.isEmpty(pk_batchcode)) {
						tabvo.setVbatchcode(null);
						tabvo.setPk_batchcode(null);
					}
					String dproducedateSQL = "SELECT dproducedate FROM scm_batchcode WHERE PK_BATCHCODE = '"+pk_batchcode+"'";
					String dvalidateSQL = "SELECT dvalidate FROM scm_batchcode WHERE PK_BATCHCODE = '"+pk_batchcode+"'";
					try {
						String dproducedate =  (String) dao.executeQuery(dproducedateSQL, new ColumnProcessor());
						String dvalidate =  (String) dao.executeQuery(dvalidateSQL, new ColumnProcessor());
						tabvo.setDproducedate(new UFDate(dproducedate));
						tabvo.setDvalidate(new UFDate(dvalidate));
					} catch (Exception e) {
						// TODO: handle exception
					}
					tabvo.setVbatchcode(vbatchcode);
					tabvo.setPk_batchcode(pk_batchcode);
					tabvo.setCstateid(cstateid);
					tabvo.setStatus(VOStatus.UPDATED);
					tabvo.setVnotebody(vnotebody);
					// 修复价格是外币时，转换价格出错问题
					tabvo.setNorigtaxmny(fsl.multiply(tabvo.getNqtorigtaxnetprice()).setScale(2, UFDouble.ROUND_HALF_UP));//价税合计 = 含税净价 * 辅数量
					tabvo.setNcaltaxmny(tabvo.getNorigtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//计税金额
					tabvo.setNorigmny(tabvo.getNorigtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//无税金额
					tabvo.setNtaxmny(fsl.multiply(tabvo.getNqttaxnetprice()).setScale(2, UFDouble.ROUND_HALF_UP));//本币价税合计
					tabvo.setNmny(tabvo.getNtaxmny().div(ntaxrate).setScale(2, UFDouble.ROUND_HALF_UP));//本币无税金额
					tabvo.setNqtunitnum(fsl);
					tabvo.setNtax(tabvo.getNtaxmny().sub(tabvo.getNmny()));

					bodylist.add(tabvo);
				}
			}
		}
		destVo.setChildrenVO(bodylist.toArray(new SaleOutBodyVO[bodylist.size()]));

		try {
			try{
//	 			防止单据转换规则出现交易类型为空
//				if (destVo.getHead().getCtrantypeid() == null) {
//					String transtype = "45-01";
//					String transtypePk = "0001N710000000001BOB";
//					destVo.getHead().setCtrantypeid(transtypePk);
//					destVo.getHead().setVtrantypecode(transtype);
//				}
				InvocationInfoProxy.getInstance().setUserId(approverID);//设置系统操作人
				saveVOs = ISaleOutMaintain.update(new SaleOutVO[] {destVo},new SaleOutVO[] {oldvo});//保存
			} catch (Exception e) {
				throw new Exception("销售出库单保存报错："+e);
			}
			try{
				if(saveVOs != null && saveVOs.length > 0) {
					ISaleOutMaintainAPI saleoutApi = NCLocator.getInstance().lookup(ISaleOutMaintainAPI.class);
					UFDateTime taudittime_t = new UFDateTime(taudittime);
					InvocationInfoProxy.getInstance().setBizDateTime(taudittime_t.getMillis());
					saveVOs = saleoutApi.signBills(saveVOs);//签字
				}
			} catch (Exception e) {
				throw new Exception("销售出库单签字报错："+e);
			}

		}catch (Exception e) {
			e.printStackTrace();
			retjson.put("ReturnMessage", "销售出库单接口报错："+e);
			return RestUtils.toJSONString(retjson);
		}


		List<String> vbillCodes = new ArrayList<>();
		if(saveVOs != null && saveVOs.length > 0) {
			for(SaleOutVO vo : saveVOs) {
				vbillCodes.add(vo.getHead().getVbillcode());
			}
			retjson.put("Status", "1");
			retjson.put("ResultBillcode", vbillCodes.get(0));
			retjson.put("ReturnMessage", "NC销售出库保存并签字成功！");
		}


		return RestUtils.toJSONString(retjson);

	}

	public JSONString WmsUpdateM4R(JSONObject str) {
		JSONObject retjson = new JSONObject();
		retjson.put("Status", "0");//默认失败

		BaseDAO dao = new BaseDAO();
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
		String cspecialhid = str.getString("cspecialhid");//盘点单主键
		String hsql = "select vbillcode from ic_invcount_h iih where cspecialhid = '"+cspecialhid+"' and fbillflag = 1";
		String vbillcode = "";
		try {
			vbillcode = (String) dao.executeQuery(hsql, new ColumnProcessor());
		} catch (DAOException e2) {
			retjson.put("ReturnMessage", "查询盘点单报错"+e2.getMessage());
			return RestUtils.toJSONString(retjson);
		}
		if(StringUtils.isEmpty(vbillcode)) {
			retjson.put("ReturnMessage", "请检查盘点单主键是否正确，盘点单状态是否正确：" + cspecialhid);
			return RestUtils.toJSONString(retjson);
		}
		JSONArray bodylists = str.getJSONArray("list");
		String msg = "盘点单："+vbillcode+",表体行：";
		for (int j = 0; j < bodylists.size(); j++) {
			JSONObject wmsbvo = bodylists.getJSONObject(j);
			IUAPQueryBS query = NCLocator.getInstance().lookup(IUAPQueryBS.class);
			String cspecialbid = wmsbvo.getString("cspecialbid");
			String sncountastnum = wmsbvo.getString("ncountastnum");
			String sncountnum = wmsbvo.getString("ncountnum");
			String sql = "select * from ic_invcount_b  where  cspecialbid ='"+ cspecialbid + "'";
			ArrayList<InvCountBodyVO> blist = new ArrayList<>();
			try {
				blist = (ArrayList<InvCountBodyVO>) query.executeQuery(sql,
						new BeanListProcessor(InvCountBodyVO.class));
			} catch (BusinessException e1) {
				retjson.put("ReturnMessage", "查询盘点单表体报错"+e1.getMessage());
				return RestUtils.toJSONString(retjson);
			}

			if (blist != null && blist.size() > 0) {
				try {
					String vchangerate = blist.get(0).getVchangerate();
					if ((StringUtils.isEmpty(vchangerate)) || sncountnum == null)
					{
						continue ;
					}

//	 				 UFDouble ncountnum = HslParseUtil.hslMultiplyUFDouble(vchangerate, new UFDouble(ncountastnum));
					UFDouble ncountnum = new UFDouble(sncountnum);

					UFDouble ncountastnum = HslParseUtil.hslDivUFDouble(vchangerate, new UFDouble(ncountnum));

					UFDouble nonhandnum = blist.get(0).getNonhandnum();

					UFDouble ndiffnum = NCBaseTypeUtils.sub(ncountnum, new UFDouble[] { nonhandnum });

					UFDouble nonhandastnum = blist.get(0).getNonhandastnum();

					UFDouble ndiffastnum = NCBaseTypeUtils.sub(new UFDouble(ncountastnum), new UFDouble[] { nonhandastnum });


					String sql2 = "update ic_invcount_b set ncountastnum ='"+ncountastnum+"',ncountnum='" + ncountnum +
							"',ndiffnum='"+ndiffnum+"',ndiffastnum='"+ndiffastnum+"',nadjustnum ='"+ndiffnum +"',vchangerate ='"+vchangerate+
							"',nadjustastnum ='"+ndiffastnum+"' where cspecialbid = '"+cspecialbid+"'";
					dao.executeUpdate(sql2.toString());
					msg = msg + "'cspecialbid',";
				} catch (DAOException e) {
					retjson.put("ReturnMessage", "更新盘点单报错"+e.getMessage());
					return RestUtils.toJSONString(retjson);
				}
				String sql1 = "update ic_invcount_h set inputmode  ='1' where cspecialhid = (select cspecialhid from ic_invcount_b where cspecialbid = '"+cspecialbid+"')";
				try {
					dao.executeUpdate(sql1.toString());
				} catch (DAOException e) {
					retjson.put("ReturnMessage", "更新盘点单报错"+e.getMessage());
					return RestUtils.toJSONString(retjson);
				}
			}else {
				retjson.put("ReturnMessage", "找不到对应的盘点单行："+sql);
				return RestUtils.toJSONString(retjson);
			}
		}
		if(msg.length()>0) {
			retjson.put("Status", "1");
			retjson.put("ReturnMessage", "盘点单回写成功:"+msg.substring(0, msg.length()-1));
			return RestUtils.toJSONString(retjson);
		}else {
			retjson.put("ReturnMessage", "盘点单回写失败,请确认回写信息是否正确:"+msg.substring(0, msg.length()-1));
			return RestUtils.toJSONString(retjson);
		}



	}

	private static String queryName(String tablename, String nameORcode,
									String pk, String pkvalue) {
		if (StringUtils.isEmpty(pkvalue)) {
			return null;
		}
		String sql = "select " + nameORcode + " from " + tablename + " where "
				+ pk + "='" + pkvalue + "' and nvl(dr,0)=0";
		IUAPQueryBS bs = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		Object result;
		try {
			result = bs.executeQuery(sql, new ColumnProcessor());
			if (null != result && !"".equals(result)) {
				return result.toString();
			} else {
				return null;
			}
		} catch (BusinessException e) {
			return null;
		}
	}

	private static String queryCtrantypeid (String tablename, String nameORcode,
											String pk, String pkvalue) {
		if (StringUtils.isEmpty(pkvalue)) {
			return null;
		}
		String sql = "select " + nameORcode + " from " + tablename + " where "
				+ pk + "='" + pkvalue + "' and nvl(dr,0)=0 and pk_group = '0001A1100000000016JO'";
		IUAPQueryBS bs = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		Object result;
		try {
			result = bs.executeQuery(sql, new ColumnProcessor());
			if (null != result && !"".equals(result)) {
				return result.toString();
			} else {
				return null;
			}
		} catch (BusinessException e) {
			return null;
		}
	}


	public JSONString WmsInsertM4N(JSONObject str) {
		JSONObject retjson = new JSONObject();
		retjson.put("Status", "0");//默认失败

		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量


		IBatchCodeQuery batchcodeQuery = NCLocator.getInstance().lookup(IBatchCodeQuery.class);
		List<TransformBodyVO> xtzhBodys = new ArrayList<>();
		TransformVO aggvo = new TransformVO();
		String pk_group = "0001A1100000000016JO";
		String pk_org = str.getString("pk_org");
		String pk_org_v = queryName("org_orgs", "pk_vid", "pk_org", pk_org);
		String dbilldate = str.getString("dbilldate");
		aggvo.setParentVO(new TransformHeadVO());
		try {
			String dmakedate = str.getString("dmakedate");
			String coutbsor = str.getString("coutbsor");
			String billmaker = str.getString("billmaker");
			String vtrantypecode = str.getString("vtrantypecode");
			String pk_dept = str.getString("cdptid");
// 	 		String pk_dept = aggvo.getHead().getCdptid();
			String pk_dept_v = queryName("org_dept", "pk_vid", "pk_dept", pk_dept);
// 	 		String vtrantypecode = "4N-01";
			String ctrantypeid = queryCtrantypeid("bd_billtype", "pk_billtypeid", "pk_billtypecode", vtrantypecode);
			String vnote = str.getString("vnote");

			BaseDAO dao = new BaseDAO();
			String sql_userid = "select u.cuserid from sm_user u where u.pk_psndoc='"+billmaker+"'";
			String userID = "";
			try {
				userID = (String) dao.executeQuery(sql_userid, new ColumnProcessor());
				if(StringUtils.isNotEmpty(userID)) {
					billmaker = userID;
				}
			} catch (DAOException e2) {
				retjson.put("ReturnMessage", "查询用户ID报错："+e2.getMessage());
				return RestUtils.toJSONString(retjson);
			}

			InvocationInfoProxy.getInstance().setUserId(billmaker);
			aggvo.getHead().setDbilldate(new UFDate(dbilldate));
			aggvo.getHead().setDmakedate(new UFDate(dmakedate));
			aggvo.getHead().setBillmaker(billmaker);
			aggvo.getHead().setCreator(billmaker);
			aggvo.getHead().setCreationtime(new UFDateTime(dbilldate));
			aggvo.getHead().setCoutbsor(coutbsor);
			aggvo.getHead().setPk_org_v(pk_org_v);
			aggvo.getHead().setPk_group(pk_group);
			aggvo.getHead().setPk_org(pk_org );
			aggvo.getHead().setCorpoid(pk_org);
			aggvo.getHead().setCorpvid(pk_org_v);
			aggvo.getHead().setCdptid(pk_dept);
			aggvo.getHead().setCdptvid(pk_dept_v);
			aggvo.getHead().setCtrantypeid(ctrantypeid);
			aggvo.getHead().setVtrantypecode(vtrantypecode);
			aggvo.getHead().setVnote(vnote);
			aggvo.getHead().setFbillflag(1);
			JSONArray bodylists = str.getJSONArray("list");
			for (int j = 0; j < bodylists.size(); j++) {
				TransformBodyVO bvo = new TransformBodyVO();
				JSONObject bjson = bodylists.getJSONObject(j);
				bvo.setPk_group(pk_group);
				bvo.setPk_org(pk_org);
				bvo.setPk_org_v(pk_org_v);
				bvo.setCorpoid(pk_org);
				bvo.setCorpvid(pk_org_v);
				bvo.setFbillrowflag(bjson.getInteger("fbillrowflag"));
				bvo.setCrowno(bjson.getString("crowno"));
				bvo.setCbodywarehouseid(bjson.getString("cbodywarehouseid"));
				bvo.setCmaterialoid(bjson.getString("cmaterialoid"));
				bvo.setCunitid(bjson.getString("cunitid"));
				bvo.setCastunitid(bjson.getString("castunitid"));
				bvo.setVchangerate(bjson.getString("vchangerate"));
				bvo.setNnum(new UFDouble(bjson.getString("nnum")));
				bvo.setNassistnum(new UFDouble(bjson.getString("nassistnum")));
				bvo.setVnotebody(bjson.getString("vnotebody"));
				bvo.setVbatchcode(bjson.getString("vbatchcode"));
				bvo.setCstateid(bjson.getString("cstateid"));
				if(bjson.getInteger("fbillrowflag")!=null && bjson.getInteger("fbillrowflag") == 3) {
					bvo.setDproducedate(new UFDate(bjson.getString("dproducedate")));
				}
				xtzhBodys.add(bvo);
			}
		} catch (Exception e) {
			retjson.put("ReturnMessage", "JSON处理报错："+ e);
			return RestUtils.toJSONString(retjson);
		}

		TransformBodyVO[] bodys = xtzhBodys.toArray(new TransformBodyVO[xtzhBodys.size()]);

		for (TransformBodyVO bodyvo : bodys) {
			bodyvo.setCmaterialvid( bodyvo.getCmaterialoid());
			BatchcodeVO[] batchcodes;
			try {
				batchcodes = batchcodeQuery.queryBatchVOs(
						new String[] { bodyvo.getCmaterialoid()},
						new String[] { bodyvo.getVbatchcode()});
				if(batchcodes != null && batchcodes.length > 0){
					bodyvo.setPk_batchcode(batchcodes[0].getPk_batchcode());// 批次号档案主键
					bodyvo.setDproducedate(batchcodes[0].getDproducedate());// 生产日期
					bodyvo.setDvalidate(batchcodes[0].getDvalidate());// 失效日期
				}
			} catch (BusinessException e) {
				retjson.put("ReturnMessage", "根据批次号["+bodyvo.getVbatchcode()+"]获取批次主键失败"+e);
				return RestUtils.toJSONString(retjson);
			}

			bodyvo.setPk_group(pk_group);
			bodyvo.setPk_org(pk_org);
			bodyvo.setPk_org_v(pk_org_v);
//			bodyvo.set
		}
		aggvo.setChildrenVO(bodys);
		ITransformMaitain iMaitain =  NCLocator.getInstance().lookup(ITransformMaitain.class);
		IPFBusiAction service = (IPFBusiAction)NCLocator.getInstance().lookup(IPFBusiAction.class);
		TransformVO[] res;
		try {
			try {
				res = iMaitain.insert(new TransformVO[]{aggvo});
				res = (TransformVO[])service.processAction("APPROVE", "4N", null, aggvo, null, null);
			} catch (BusinessException e1) {
				throw new Exception("形态转换单保存审批报错："+e1);
			}
//  	iappApi.approve(new TransformVO[]{aggvo}, null);
			TransformVO outaggvo = (TransformVO) aggvo.clone();
			TransformVO inaggvo = (TransformVO) aggvo.clone();
			List<TransformBodyVO> outbodys = new ArrayList<>();
			List<TransformBodyVO> inbodys = new ArrayList<>();
			for(TransformBodyVO nbvo : aggvo.getBodys()){
				if(nbvo.getFbillrowflag()!=null && nbvo.getFbillrowflag()==2){
					outbodys.add(nbvo);
				}else{
					inbodys.add(nbvo);
				}
			}
			outaggvo.setChildrenVO(outbodys.toArray(new TransformBodyVO[outbodys.size()]));
			inaggvo.setChildrenVO(inbodys.toArray(new TransformBodyVO[inbodys.size()]));

			IPfExchangeService ipfes = NCLocator.getInstance().lookup(IPfExchangeService.class);
			GeneralInVO agginvo = null;
			GeneralOutVO aggoutvo = null;
			UFDateTime intime = new UFDateTime();
			try {
				agginvo = (GeneralInVO)ipfes.runChangeData("4N", "4A",inaggvo , null);
				aggoutvo = (GeneralOutVO)ipfes.runChangeData("4N", "4I",outaggvo , null);
				String cintrantypeid = queryCtrantypeid("bd_billtype", "pk_billtypeid", "pk_billtypecode", "4A-06");
				agginvo.getHead().setCtrantypeid(cintrantypeid);
				agginvo.getHead().setVtrantypecode("4A-06");
				String couttrantypeid = queryCtrantypeid("bd_billtype", "pk_billtypeid", "pk_billtypecode", "4I-06");
				aggoutvo.getHead().setCtrantypeid(couttrantypeid);
				aggoutvo.getHead().setVtrantypecode("4I-06");
				agginvo.getHead().setPk_org_v(pk_org_v);
				aggoutvo.getHead().setPk_org_v(pk_org_v);
				for (int i = 0; i < aggoutvo.getBodys().length; i++) {
					GeneralOutBodyVO outbvo = aggoutvo.getBodys()[i];
					outbvo.setCrowno(i*10+"");
				}
				for (int i = 0; i < agginvo.getBodys().length; i++) {
					GeneralInBodyVO inbvo = agginvo.getBodys()[i];
					inbvo.setCrowno(i*10+"");
				}
				intime = new UFDateTime(dbilldate.substring(0, 11)+ new UFDateTime().getTime());
				processBizDate(aggoutvo,intime);

			} catch (BusinessException e1) {
				throw new Exception("形态转换生成其它出入库单失败："+e1);
			}

			IGeneralOutMaintainAPI outApi = NCLocator.getInstance().lookup(IGeneralOutMaintainAPI.class);
			IGeneralInMaintainAPI inApi = NCLocator.getInstance().lookup(IGeneralInMaintainAPI.class);
			try {
				InvocationInfoProxy.getInstance().setBizDateTime(intime.getMillis());
				GeneralOutVO[] returnaggvos = outApi.insertBills(new GeneralOutVO[]{aggoutvo});
				returnaggvos = outApi.signBills(returnaggvos);
				retjson.put("m4iBillcode",returnaggvos[0].getHead().getVbillcode() );
			} catch (Exception e) {
				throw new Exception("其它出库单保存签字失败："+e);
			}

			try {
				Thread.sleep(2000);
				UFDateTime outtime = new UFDateTime(dbilldate.substring(0, 11)+ new UFDateTime().getTime());
				InvocationInfoProxy.getInstance().setBizDateTime(outtime.getMillis());
				processBizDate(aggoutvo,outtime);
				fillDproducedate2VO(agginvo);
				GeneralInVO[] returninaggvos = inApi.insertBills(new GeneralInVO[]{agginvo});
				returninaggvos = inApi.signBills(returninaggvos);
				retjson.put("Status", "1");
				retjson.put("m4aBillcode",returninaggvos[0].getHead().getVbillcode() );
				retjson.put("ReturnMessage", "形态转换处理成功！");
				return RestUtils.toJSONString(retjson);
			} catch (Exception e) {
				throw new Exception("其它入库单保存签字失败："+e);
			}

		} catch (Exception e) {
			retjson.put("ReturnMessage", "单据处理失败："+e);
			return RestUtils.toJSONString(retjson);
		}
	}

	public JSONString WmsAddM4nBody(JSONObject str) {
		JSONObject retjson = new JSONObject();
		retjson.put("Status", "0");//默认失败

		BaseDAO dao = new BaseDAO();
		NCLocator.getInstance().lookup(ISecurityTokenCallback.class).token("NCSystem".getBytes(),"pfxx".getBytes());
		InvocationInfoProxy.getInstance().setUserDataSource(getValue("datasource"));
		InvocationInfoProxy.getInstance().setGroupId("0001A1100000000016JO");  //设置集团环境变量
		String cspecialhid = str.getString("cspecialhid");//盘点单主键
		IInvCountAdjust queryInvCount = NCLocator.getInstance().lookup(IInvCountAdjust.class);
		IInvCountMaintain saveInvCount = NCLocator.getInstance().lookup(IInvCountMaintain.class);
		InvCountBillVO[] invCountVOs;
		try {
			invCountVOs = queryInvCount.queryInvCountByIds(new String[]{cspecialhid});
			if(invCountVOs == null || invCountVOs.length == 0){
				retjson.put("ReturnMessage", "根据主键获取不到对应的盘点单,主键：" + cspecialhid);
				return RestUtils.toJSONString(retjson);
			}
		} catch (BusinessException e3) {
			retjson.put("ReturnMessage", "根据主键获取盘点单报错：" + e3);
			return RestUtils.toJSONString(retjson);
		}

		InvCountBillVO newInvCount = (InvCountBillVO) invCountVOs[0].clone();
		InvCountBodyVO[] bodys = invCountVOs[0].getBodys();
		List<InvCountBodyVO> bvolists = new ArrayList<InvCountBodyVO>();
		UFDouble maxoldcrowno = new UFDouble(0);
		for (int i = 0; i < bodys.length; i++) {
			String oldcrowno = bodys[i].getCrowno();
			UFDouble doldcrowno = new UFDouble(oldcrowno);
			if(doldcrowno.compareTo(maxoldcrowno)>0) {
				maxoldcrowno = doldcrowno;
			}
			bvolists.add(bodys[i]);
		}
		InvCountBodyVO oldbody = bodys[0];
		InvCountBodyVO newbody = new InvCountBodyVO();
		JSONObject wmsbvo = str;
		String material = wmsbvo.getString("cmaterialoid");//物料
		String casscustid = wmsbvo.getString("casscustid");//客户
		String castunitid = wmsbvo.getString("castunitid");//辅单位
		String cunitid = wmsbvo.getString("cunitid");//主单位
		String vchangerate = wmsbvo.getString("vchangerate");//换算率
		String batchcode = wmsbvo.getString("batchcode");//批次号
		String sncountastnum = wmsbvo.getString("ncountastnum");//辅数量
		String sncountnum = wmsbvo.getString("ncountnum");//主数量
		String cstateid = wmsbvo.getString("cstateid");
		UFDouble dncountastnum = new UFDouble(sncountastnum);
		UFDouble dncountnum = new UFDouble(sncountnum);
		IBatchCodeQuery batchcodeQuery = getInBatchCodeQueryService();
		if(StringUtils.isNotEmpty(batchcode)){
			newbody.setVbatchcode(batchcode);
			BatchcodeVO[] batchcodes;
			try {
				batchcodes = batchcodeQuery.queryBatchVOs(
						new String[] {material},
						new String[] {batchcode});
				if(batchcodes != null && batchcodes.length > 0){
					newbody.setPk_batchcode(batchcodes[0].getPk_batchcode());// 批次号档案主键
					newbody.setDproducedate(batchcodes[0].getDproducedate());// 生产日期
					newbody.setDvalidate(batchcodes[0].getDvalidate());// 失效日期
					newbody.setCqualitylevelid(batchcodes[0].getCqualitylevelid());
				}
			} catch (BusinessException e) {
				retjson.put("ReturnMessage", "根据批次号["+batchcode+"]获取批次主键失败"+e);
				return RestUtils.toJSONString(retjson);
			}
		}

		if(StringUtils.isNotEmpty(cstateid)) {
			newbody.setCstateid(cstateid);
		}
		int newcrowno = maxoldcrowno.toDouble().intValue();
		String crowno = (newcrowno+10)+"";
		newbody.setCmaterialoid(material);
		newbody.setCmaterialvid(material);
		newbody.setCrowno(crowno);
		newbody.setCasscustid(casscustid);
		newbody.setCastunitid(castunitid);
		newbody.setCspecialhid(cspecialhid);
		newbody.setCunitid(cunitid);
		newbody.setNadjustastnum(dncountastnum);
		newbody.setNadjustnum(dncountnum);
		newbody.setNcountastnum(dncountastnum);
		newbody.setNcountnum(dncountnum);
		newbody.setNdiffastnum(dncountastnum);
		newbody.setNdiffnum(dncountnum);
		newbody.setPk_group(oldbody.getPk_group());
		newbody.setPk_org(oldbody.getPk_org());
		newbody.setPk_org_v(oldbody.getPk_org_v());
		newbody.setVchangerate(vchangerate);
		newbody.setStatus(2);
		bvolists.add(newbody);
		InvCountBodyVO[] newbvos = bvolists.toArray(new InvCountBodyVO[bvolists.size()]);
		newInvCount.setChildrenVO(newbvos);
		try {
			InvCountBillVO[] reVos = saveInvCount.update(new InvCountBillVO[]{newInvCount}, invCountVOs);
			if(reVos!=null && reVos.length > 0) {
				InvCountBodyVO[] rebvos = reVos[0].getBodys();
				for(InvCountBodyVO rebvo : rebvos) {
					if(rebvo.getCrowno().equals(crowno)) {
						retjson.put("Status", "1");
						retjson.put("newbodyid",rebvo.getCspecialbid() );
						retjson.put("ReturnMessage", "盘点明细新增成功！");
						return RestUtils.toJSONString(retjson);
					}
				}
			}
		} catch (BusinessException e) {
			retjson.put("ReturnMessage", "插入盘点单表体行报错："+e);
			return RestUtils.toJSONString(retjson);
		}
		retjson.put("ReturnMessage", "未知错误，传入JSON数据："+str);
		return RestUtils.toJSONString(retjson);
	}

	private IBatchCodeQuery getInBatchCodeQueryService() {
		return NCLocator.getInstance().lookup(IBatchCodeQuery.class);
	}

	private void fillDproducedate2VO(GeneralInVO vo) {
		Map<String, InvCalBodyVO> orgMat2Qual = this.getOrgMat2Qualitynum(vo);
		String pk_org = vo.getHead().getPk_org();
		GeneralInBodyVO[] bodys =vo.getBodys();
		for (GeneralInBodyVO body : bodys) {
			InvCalBodyVO calbodyVO = orgMat2Qual.get(pk_org + body.getCmaterialvid());
			if (body.getDproducedate() != null && body.getDvalidate() == null && calbodyVO != null) {
				body.setDvalidate(DateCalUtil.calDvalidate(body.getDproducedate(), calbodyVO.getQualitynum(), calbodyVO.getQualityunit()));
			}
		}
	}

	private Map<String, InvCalBodyVO> getOrgMat2Qualitynum(GeneralInVO bill) {
		Map<String, InvCalBodyVO> orgMat2Qual = new HashMap<String, InvCalBodyVO>();
		Map<String, List<String>> org2matVID = new HashMap<String, List<String>>();
		String pk_org = bill.getHead().getPk_org();
		List<String> cmaterialVIDs = org2matVID.get(pk_org);
		if (cmaterialVIDs == null) {
			cmaterialVIDs = new ArrayList<String>();
			org2matVID.put(pk_org, cmaterialVIDs);
		}
		for (GeneralInBodyVO body : bill.getBodys()) {
			if (body.getStatus() == VOStatus.DELETED
					|| cmaterialVIDs.contains(body.getCmaterialvid())) {
				continue;
			}
			cmaterialVIDs.add(body.getCmaterialvid());
		}
		if (org2matVID.isEmpty()) {
			return orgMat2Qual;
		}
		for (Entry<String, List<String>> entry : org2matVID.entrySet()) {
			Map<String, InvCalBodyVO> invcal = this.fetchMaterialInfo(entry.getKey(), entry.getValue());
			for (Entry<String, InvCalBodyVO> inv : invcal.entrySet()) {
				orgMat2Qual.put(entry.getKey() + inv.getKey(), inv.getValue());
			}
		}
		return orgMat2Qual;
	}

	private Map<String, InvCalBodyVO> fetchMaterialInfo(String pk_org,
														List<String> cmaterialVIDs) {
		Map<String, InvCalBodyVO> retMap = new HashMap<String, InvCalBodyVO>();
		ICBSContext context = new ICBSContext();
		InvCalBodyVO[] stockVOs =
				context.getInvInfo().getInvCalBodyVO(pk_org,
						cmaterialVIDs.toArray(new String[cmaterialVIDs.size()]));
		if (stockVOs == null || stockVOs.length == 0) {
			return retMap;
		}
		for (InvCalBodyVO vo : stockVOs) {
			retMap.put(vo.getPk_material(), vo);
		}
		return retMap;
	}

	private void processBizDate(ICBillVO bill,UFDateTime date)
	{
		for (ICBillBodyVO body : bill.getBodys()) {
			if ((body.getNnum() != null) || (body.getNassistnum() != null)) {
				body.setDbizdate(date.getDate());
			}
		}
	}

}