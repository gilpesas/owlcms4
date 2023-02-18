/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.competition.Competition;

@SuppressWarnings("serial")
public class JXLSCardsDocs extends JXLSWorkbookStreamSource {

	private final static Logger logger = LoggerFactory.getLogger(JXLSCardsDocs.class);

	public JXLSCardsDocs() {
		super();
	}

	private void setPageBreaks(Workbook workbook, int cardsPerPage, int linesPerCard) {
		Sheet sheet = workbook.getSheetAt(0);
		int lastRowNum = sheet.getLastRowNum();
		sheet.setAutobreaks(false);
		int increment = cardsPerPage * linesPerCard + (cardsPerPage - 1);

		for (int curRowNum = increment; curRowNum < lastRowNum;) {
			logger.debug("setting break on line {}", curRowNum);
			sheet.setRowBreak(curRowNum - 1);
			curRowNum += increment;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
	 * postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
		if (Competition.getCurrent().getComputedCardsTemplateFileName().contains("IWF-")) {
			setPageBreaks(workbook, 1, 17);
		} else if (Competition.getCurrent().getComputedCardsTemplateFileName().contains("SmallCards")) {
			setPageBreaks(workbook, 2, 10);
		}
	}

}
