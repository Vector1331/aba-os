package com.aba.os.abaosserver.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 마이그레이션 테스트용 엑셀 파일 생성 유틸리티
 * 실행: ./gradlew test --tests "*.TestExcelGenerator.generateTestExcel"
 */
public class TestExcelGenerator {

    @Test
    public void generateTestExcel() throws IOException {
        // 출력 디렉토리 생성
        Path outputDir = Paths.get("src/test-data");
        Files.createDirectories(outputDir);

        Path filePath = outputDir.resolve("migration_data.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Children
            createChildrenSheet(workbook);

            // Sheet 2: Goals
            createGoalsSheet(workbook);

            // 파일 저장
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }

        System.out.println("파일 생성 위치: " + filePath.toAbsolutePath());
    }

    private static void createChildrenSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Children");

        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 헤더 행
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Name", "BirthDate", "Gender", "Diagnosis"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행
        Object[][] data = {
                {"홍길동", "2019-03-15", "M", "자폐 스펙트럼 장애 (ASD Level 1)"},
                {"김민지", "2020-07-22", "F", "발달지연"},
                {"이준호", "2018-11-03", "M", "ADHD"},
                {"박서연", "2021-01-10", "F", "언어발달지연"},
                {"최민수", "2019-08-25", "M", "자폐 스펙트럼 장애 (ASD Level 2)"},
                {"정유진", "2020-04-18", "F", "감각처리장애"},
                {"강도윤", "2017-12-30", "M", "지적장애 경계선"},
                {"윤서아", "2021-06-05", "F", "전반적 발달지연"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue((String) data[i][j]);
            }
        }

        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createGoalsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Goals");

        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 헤더 행
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ChildName", "GoalContent", "Category"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행
        Object[][] data = {
                // 홍길동 목표
                {"홍길동", "이름 호명 시 3초간 눈 맞춤", "SOCIAL"},
                {"홍길동", "간단한 지시 따르기 (앉아, 일어나)", "COMMUNICATION"},
                {"홍길동", "차례 기다리기", "BEHAVIOR"},

                // 김민지 목표
                {"김민지", "요청하기 제스처 사용 (손 내밀기)", "COMMUNICATION"},
                {"김민지", "또래와 장난감 공유하기", "SOCIAL"},

                // 이준호 목표
                {"이준호", "과제 수행 시 자리에 앉아있기 (5분)", "BEHAVIOR"},
                {"이준호", "감정 인식하기 (기쁨, 슬픔)", "COGNITIVE"},

                // 박서연 목표
                {"박서연", "두 단어 조합하여 말하기", "COMMUNICATION"},
                {"박서연", "그림책 보며 사물 이름 말하기", "COGNITIVE"},

                // 최민수 목표
                {"최민수", "인사하기 (안녕하세요)", "사회성"},
                {"최민수", "물건 요청하기 (주세요)", "의사소통"},

                // 정유진 목표
                {"정유진", "다양한 질감 탐색하기", "SENSORY"},
                {"정유진", "신발 신기", "SELF_CARE"},

                // 강도윤 목표
                {"강도윤", "숫자 1-10 세기", "인지"},
                {"강도윤", "블록 5개 쌓기", "MOTOR"},

                // 윤서아 목표
                {"윤서아", "역할 놀이 참여하기", "PLAY"},
                {"윤서아", "양치질 스스로 하기", "신변처리"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue((String) data[i][j]);
            }
        }

        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
