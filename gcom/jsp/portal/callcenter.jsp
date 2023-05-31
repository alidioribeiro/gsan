<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/struts-template.tld" prefix="template"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN">

<html:html>

<head>

<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="viewport" content="height=device-height, initial-scale=1.0">

<title>Cosanpa - Loja Virtual</title>

<link href="<bean:message key="caminho.portal.css"/>portal.css" rel="stylesheet">

</head>

<body>
	<%@ include file="/jsp/portal/cabecalho.jsp"%>

<%-- 	<%@ include file="/jsp/portal/acesso-barra.jsp"%> --%>
	
	<div class="page-wrap">
		<div class="container pagina">
			<div class="container container-breadcrumb">
				<ul class="breadcrumb">
					<li class="breadcrumb-item"><a href="portal.do">P�gina Inicial</a></li>
					<li class="breadcrumb-item active">Call Center</li>
				</ul>
			</div>
	
			<div class="pagina-titulo">
				<h2>Call Center</h2>
			</div>
	
			<div class="container">
				<div class="row">
					<div class="callcenter-info col-sm-6">
						<p class="text-center"><i class="fa fa-phone fa-2x"></i><span>0800 0195 195</span></p>
					</div>
					<div class="callcenter-info col-sm-6">
						<p class="text-center"><i class="fa fa-whatsapp fa-2x"></i><span>(91)98814-5721</span></p>
					</div>
				</div>
			</div>
			
			<div class="pagina-conteudo">
				<h3>Sobre</h3>
			
				<p>A Central de Atendimento Telef�nico da COSANPA, al�m do servi�o de Call Center, recebe as demandas de seus clientes via WhatsApp e Chat
					diretamente nos monitores dos Atendentes de Call Center que dar�o tratamento �s solicita��es.</p>
	
				<p>Essas informa��es ser�o inseridas no Sistema GSAN atrav�s de Registro de Atendimento contendo principalmente matr�cula, CPF, n�mero de
					telefone, email para atualiza��o cadastral, podendo anexar imagens de documentos e fotos de vazamentos, hidr�metro e outras que sejam necess�rias
					para o bom atendimento. Sendo, ao final do registro, informado ao Cliente o protocolo para acompanhamento.</p>
	
				<p>O sistema tamb�m pode ser utilizado para encaminhamento de documentos de cobran�a e fatura de �gua conforme a solicita��o do cliente.</p>
	
				<!-- 			<br> -->
	
				<!-- 			<div class="media"> -->
				<!-- 				<img class="align-self-center mr-3" src="/gsan/imagens/portal/general/teleatendimento.bmp" alt="Generic placeholder image"> -->
				<!-- 				<div class="media-body"> -->
				<!-- 					<p>Essas informa��es ser�o inseridas no Sistema GSAN atrav�s de Registro de Atendimento contendo principalmente matr�cula, CPF, n�mero de -->
				<!-- 						telefone, email para atualiza��o cadastral, podendo anexar imagens de documentos e fotos de vazamentos, hidr�metro e outras que sejam -->
				<!-- 						necess�rias para o bom atendimento. Sendo, ao final do registro, informado ao Cliente o protocolo para acompanhamento.</p> -->
	
				<!-- 					<p>O sistema tamb�m pode ser utilizado para encaminhamento de documentos de cobran�a e fatura de �gua conforme a solicita��o do cliente.</p> -->
				<!-- 				</div> -->
				<!-- 			</div> -->
			</div>
		</div>
	</div>

	<%@ include file="/jsp/portal/rodape.jsp"%>
</body>
</html:html>