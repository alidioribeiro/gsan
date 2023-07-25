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
	
 	<logic:present name="nomeUsuario">
		<%@ include file="/jsp/portal/acesso-barra.jsp"%>
	</logic:present>
	
	<div class="page-wrap">
		<div class="container pagina">
			<div class="container container-breadcrumb">
				<ul class="breadcrumb">
					<li class="breadcrumb-item"><a href="portal.do">P�gina Inicial</a></li>
					<li class="breadcrumb-item active">Ouvidoria</li>
				</ul>
			</div>
			
			<div class="pagina-titulo">
				<h2>Ouvidoria</h2>
			</div>
			
			<div class="pagina-conteudo">
				<h3>O que �?</h3>
			
				<p>� o canal de relacionamento da empresa com a sociedade para atender as sugest�es, reclama��es, elogios e den�ncias de usu�rios referentes
					aos servi�os de abastecimento de �gua e de esgotamento sanit�rio prestados pela Companhia. E se estende aos empregados da Companhia.</p>
	
				<p>A Ouvidoria � acionada quando o cidad�o n�o se sente totalmente atendido em seus direitos, por outros canais dispon�veis �s suas
					reivindica��es.</p>
			</div>
			
			<div class="pagina-conteudo">
				<h3>Miss�o</h3>
	
				<p>Assegurar o direito de manifesta��o, garantir o direito � informa��o e sugerir medidas de aprimoramento com a busca de solu��es para os
					problemas apontados.</p>
			</div>
			
			<div class="pagina-conteudo">
				<h3>Como acessar</h3>
	
				<p>O acesso ser� garantido atrav�s dos links abaixo, onde � disponibilizado um formul�rio pr�prio para o relato do usu�rio.</p>
				
				<div class="links-ouvidoria text-center">
					<a href="http://ouvidoria.cosanpa.pa.gov.br/OuvidoriaCliente/formulario.jsf" target="_blank">Clique aqui para fazer Reclama��es</a>
					<br>
					<a href="http://ouvidoria.cosanpa.pa.gov.br/OuvidoriaCliente/formularioalt.jsf" target="_blank">Clique aqui para fazer Sugest�es, Elogios ou Den�ncias</a>
				</div>
				
				<p><b>OBS.:</b> Para ter acesso � Ouvidoria, em caso de reclama��es, � necess�rio primeiro o acesso ao atendimento para gerar um n�mero de protocolo.</p>
				
			</div>
		</div>
	</div>
	
	<%@ include file="/jsp/portal/rodape.jsp"%>
</body>
</html:html>