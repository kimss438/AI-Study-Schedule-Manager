import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class gui extends JFrame {
    private final ScheduleManager manager = new ScheduleManager();
    private final OpenAIService openAIService = new OpenAIService();

    private JTextField subjectField;
    private JTextField titleField;
    private JTextField deadlineField;
    private JTextField hoursField;
    private JTextField progressField;
    private JComboBox<String> importanceBox;
    private JComboBox<String> studyTypeBox;
    private JTextArea detailArea;

    private DefaultTableModel tableModel;
    private JTable taskTable;
    private JTextArea outputArea;
    private JButton aiPlanButton;

    private ArrayList<Task> shownTasks = new ArrayList<>();

    public gui() {
        setTitle("AI Study Schedule Manager");
        setSize(1250, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        root.add(createInputPanel(), BorderLayout.NORTH);
        root.add(createCenterPanel(), BorderLayout.CENTER);

        setContentPane(root);

        showAllTasks();
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new TitledBorder("할 일 입력"));

        JPanel infoPanel = new JPanel(new GridLayout(2, 7, 8, 4));

        infoPanel.add(new JLabel("과목명"));
        infoPanel.add(new JLabel("할 일 제목"));
        infoPanel.add(new JLabel("학습 유형"));
        infoPanel.add(new JLabel("마감일"));
        infoPanel.add(new JLabel("중요도"));
        infoPanel.add(new JLabel("예상 시간"));
        infoPanel.add(new JLabel("진행률"));

        subjectField = new JTextField();
        titleField = new JTextField();

        studyTypeBox = new JComboBox<>(new String[]{
                "과제",
                "시험공부",
                "복습",
                "프로젝트",
                "발표 준비",
                "기타"
        });

        deadlineField = new JTextField("2026-06-30");

        importanceBox = new JComboBox<>(new String[]{
                "1", "2", "3", "4", "5"
        });
        importanceBox.setSelectedItem("3");

        hoursField = new JTextField("1");
        progressField = new JTextField("0");

        infoPanel.add(subjectField);
        infoPanel.add(titleField);
        infoPanel.add(studyTypeBox);
        infoPanel.add(deadlineField);
        infoPanel.add(importanceBox);
        infoPanel.add(hoursField);
        infoPanel.add(progressField);

        JPanel detailPanel = new JPanel(new BorderLayout(5, 5));
        detailPanel.add(new JLabel("세부 내용 (시험 범위, 약한 부분, 해야 할 작업 등을 입력)"), BorderLayout.NORTH);

        detailArea = new JTextArea(3, 20);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setToolTipText("예: 이진트리 순회와 BST 삭제가 약함. 기출문제 2개 풀기.");

        detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton addButton = new JButton("할 일 추가");
        JButton allButton = new JButton("전체 목록");
        JButton priorityButton = new JButton("오늘의 우선순위");
        JButton updateButton = new JButton("진행률 수정");
        JButton completeButton = new JButton("완료 처리");
        JButton saveButton = new JButton("저장");
        JButton loadButton = new JButton("불러오기");
        aiPlanButton = new JButton("AI 학습 계획 생성");

        addButton.addActionListener(e -> addTask());
        allButton.addActionListener(e -> showAllTasks());
        priorityButton.addActionListener(e -> showPriorityTasks());
        updateButton.addActionListener(e -> updateProgress());
        completeButton.addActionListener(e -> completeTask());
        saveButton.addActionListener(e -> saveTasks());
        loadButton.addActionListener(e -> loadTasks());
        aiPlanButton.addActionListener(e -> generateAIPlan());

        buttonPanel.add(addButton);
        buttonPanel.add(allButton);
        buttonPanel.add(priorityButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(aiPlanButton);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        centerPanel.add(detailPanel, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JSplitPane createCenterPanel() {
        String[] columns = {
                "번호", "과목", "할 일", "유형", "마감일",
                "중요도", "예상 시간", "진행률", "우선순위", "상태"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(25);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedTaskInfo();
            }
        });

        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBorder(new TitledBorder("할 일 목록"));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(new Font("Dialog", Font.PLAIN, 15));
        outputArea.setText("할 일을 추가하고, 표에서 항목을 선택하면 세부 내용을 볼 수 있습니다.");

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(new TitledBorder("세부 내용 및 AI 학습 계획"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tableScroll,
                outputScroll
        );

        splitPane.setDividerLocation(730);

        return splitPane;
    }

    private void addTask() {
        try {
            String subject = subjectField.getText().trim();
            String title = titleField.getText().trim();
            String studyType = (String) studyTypeBox.getSelectedItem();
            String detail = detailArea.getText().trim();

            LocalDate deadline = LocalDate.parse(deadlineField.getText().trim());
            int importance = Integer.parseInt((String) importanceBox.getSelectedItem());
            double hours = Double.parseDouble(hoursField.getText().trim());
            int progress = Integer.parseInt(progressField.getText().trim());

            if (subject.isEmpty() || title.isEmpty()) {
                showMessage("과목명과 할 일 제목을 입력하세요.");
                return;
            }

            if (hours <= 0) {
                showMessage("예상 시간은 0보다 커야 합니다.");
                return;
            }

            if (progress < 0 || progress > 100) {
                showMessage("진행률은 0부터 100까지 입력하세요.");
                return;
            }

            Task task = new Task(
                    subject,
                    title,
                    studyType,
                    detail,
                    deadline,
                    importance,
                    hours,
                    progress
            );

            manager.addTask(task);

            subjectField.setText("");
            titleField.setText("");
            detailArea.setText("");
            hoursField.setText("1");
            progressField.setText("0");

            showAllTasks();
            outputArea.setText("할 일이 추가되었습니다.");

        } catch (Exception e) {
            showMessage("입력 형식을 확인하세요.\n마감일 예시: 2026-06-30");
        }
    }

    private void showAllTasks() {
        manager.calculateAllPriorities();
        refreshTable(manager.getTasks());

        outputArea.setText("[전체 할 일 목록]\n등록한 순서대로 표시됩니다.");
    }

    private void showPriorityTasks() {
        if (!manager.hasTasks()) {
            showMessage("등록된 할 일이 없습니다.");
            return;
        }

        refreshTable(manager.getSortedTasks());

        outputArea.setText("[오늘의 우선순위]\n"
                + "마감일, 중요도, 예상 시간, 진행률을 기준으로 정렬했습니다.");
    }

    private void updateProgress() {
        Task task = getSelectedTask();

        if (task == null) {
            showMessage("수정할 할 일을 표에서 선택하세요.");
            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                "새 진행률을 입력하세요. (0~100)",
                task.getProgress()
        );

        if (input == null) {
            return;
        }

        try {
            int progress = Integer.parseInt(input);

            if (progress < 0 || progress > 100) {
                showMessage("진행률은 0부터 100까지 입력하세요.");
                return;
            }

            task.setProgress(progress);
            manager.calculateAllPriorities();
            refreshTable(shownTasks);

            outputArea.setText(task.getTitle() + "의 진행률이 "
                    + progress + "%로 수정되었습니다.");

        } catch (NumberFormatException e) {
            showMessage("숫자를 입력하세요.");
        }
    }

    private void completeTask() {
        Task task = getSelectedTask();

        if (task == null) {
            showMessage("완료 처리할 할 일을 표에서 선택하세요.");
            return;
        }

        task.complete();
        manager.calculateAllPriorities();
        refreshTable(shownTasks);

        outputArea.setText(task.getTitle() + "이(가) 완료 처리되었습니다.");
    }

    private void saveTasks() {
        outputArea.setText(manager.saveTasks("tasks.txt"));
    }

    private void loadTasks() {
        String result = manager.loadTasks("tasks.txt");
        showAllTasks();
        outputArea.setText(result);
    }

    private void generateAIPlan() {
        if (!manager.hasActiveTasks()) {
            showMessage("완료되지 않은 할 일이 없습니다.");
            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                "오늘 공부 가능한 시간을 입력하세요. (예: 3 또는 1.5)",
                "3"
        );

        if (input == null) {
            return;
        }

        double availableHours;

        try {
            availableHours = Double.parseDouble(input);

            if (availableHours <= 0) {
                showMessage("0보다 큰 시간을 입력하세요.");
                return;
            }

        } catch (NumberFormatException e) {
            showMessage("숫자를 입력하세요.");
            return;
        }

        String priorityText = manager.buildPriorityText();

        aiPlanButton.setEnabled(false);
        outputArea.setText("[AI 학습 계획 생성 중...]\n잠시만 기다려주세요.");

        new SwingWorker<String, Void>() {
            protected String doInBackground() {
                return openAIService.generateStudyPlan(priorityText, availableHours);
            }

            protected void done() {
                try {
                    outputArea.setText("[AI 추천 학습 계획]\n\n" + get());
                } catch (Exception e) {
                    outputArea.setText("AI 학습 계획 생성 중 오류가 발생했습니다.");
                }

                aiPlanButton.setEnabled(true);
            }
        }.execute();
    }

    private void showSelectedTaskInfo() {
        Task task = getSelectedTask();

        if (task == null) {
            return;
        }

        String detail = task.getDetail();

        if (detail.isBlank()) {
            detail = "입력된 세부 내용이 없습니다.";
        }

        outputArea.setText(
                "[선택한 할 일 정보]\n\n"
                        + "과목명: " + task.getSubject() + "\n"
                        + "할 일: " + task.getTitle() + "\n"
                        + "학습 유형: " + task.getStudyType() + "\n"
                        + "마감일: " + task.getDeadline() + "\n"
                        + "진행률: " + task.getProgress() + "%\n\n"
                        + "[세부 내용]\n"
                        + detail
        );
    }

    private Task getSelectedTask() {
        int row = taskTable.getSelectedRow();

        if (row == -1 || row >= shownTasks.size()) {
            return null;
        }

        return shownTasks.get(row);
    }

    private void refreshTable(ArrayList<Task> taskList) {
        tableModel.setRowCount(0);
        shownTasks = new ArrayList<>(taskList);

        int number = 1;

        for (Task task : shownTasks) {
            tableModel.addRow(new Object[]{
                    number,
                    task.getSubject(),
                    task.getTitle(),
                    task.getStudyType(),
                    task.getDeadline(),
                    task.getImportance(),
                    task.getEstimatedHours() + "시간",
                    task.getProgress() + "%",
                    task.getPriorityScore() + "점",
                    task.isCompleted() ? "완료" : "진행 중"
            });

            number++;
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            gui app = new gui();
            app.setVisible(true);
        });
    }

    static class Task {
        private String subject;
        private String title;
        private String studyType;
        private String detail;
        private LocalDate deadline;
        private int importance;
        private double estimatedHours;
        private int progress;
        private int priorityScore;
        private boolean completed;

        public Task(
                String subject,
                String title,
                String studyType,
                String detail,
                LocalDate deadline,
                int importance,
                double estimatedHours,
                int progress
        ) {
            this.subject = subject;
            this.title = title;
            this.studyType = studyType;
            this.detail = detail;
            this.deadline = deadline;
            this.importance = importance;
            this.estimatedHours = estimatedHours;
            this.progress = progress;
            this.priorityScore = 0;
            this.completed = progress >= 100;
        }

        public Task(
                String subject,
                String title,
                String studyType,
                String detail,
                LocalDate deadline,
                int importance,
                double estimatedHours,
                int progress,
                boolean completed
        ) {
            this.subject = subject;
            this.title = title;
            this.studyType = studyType;
            this.detail = detail;
            this.deadline = deadline;
            this.importance = importance;
            this.estimatedHours = estimatedHours;
            this.progress = progress;
            this.priorityScore = 0;
            this.completed = completed;
        }

        public String getSubject() {
            return subject;
        }

        public String getTitle() {
            return title;
        }

        public String getStudyType() {
            return studyType;
        }

        public String getDetail() {
            return detail;
        }

        public LocalDate getDeadline() {
            return deadline;
        }

        public int getImportance() {
            return importance;
        }

        public double getEstimatedHours() {
            return estimatedHours;
        }

        public int getProgress() {
            return progress;
        }

        public int getPriorityScore() {
            return priorityScore;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setPriorityScore(int priorityScore) {
            this.priorityScore = priorityScore;
        }

        public void setProgress(int progress) {
            this.progress = progress;
            this.completed = progress >= 100;
        }

        public void complete() {
            progress = 100;
            completed = true;
            priorityScore = 0;
        }

        private static String encode(String value) {
            return Base64.getEncoder().encodeToString(
                    value.getBytes(StandardCharsets.UTF_8)
            );
        }

        private static String decode(String value) {
            return new String(
                    Base64.getDecoder().decode(value),
                    StandardCharsets.UTF_8
            );
        }

        public String toFileString() {
            return encode(subject) + "\t"
                    + encode(title) + "\t"
                    + encode(studyType) + "\t"
                    + encode(detail) + "\t"
                    + deadline + "\t"
                    + importance + "\t"
                    + estimatedHours + "\t"
                    + progress + "\t"
                    + completed;
        }

        public static Task fromFileString(String line) {
            String[] data = line.split("\t", -1);

            if (data.length == 7) {
                return new Task(
                        data[0],
                        data[1],
                        "기타",
                        "",
                        LocalDate.parse(data[2]),
                        Integer.parseInt(data[3]),
                        Double.parseDouble(data[4]),
                        Integer.parseInt(data[5]),
                        Boolean.parseBoolean(data[6])
                );
            }

            return new Task(
                    decode(data[0]),
                    decode(data[1]),
                    decode(data[2]),
                    decode(data[3]),
                    LocalDate.parse(data[4]),
                    Integer.parseInt(data[5]),
                    Double.parseDouble(data[6]),
                    Integer.parseInt(data[7]),
                    Boolean.parseBoolean(data[8])
            );
        }
    }

    static class ScheduleManager {
        private final ArrayList<Task> tasks = new ArrayList<>();

        public void addTask(Task task) {
            tasks.add(task);
        }

        public boolean hasTasks() {
            return !tasks.isEmpty();
        }

        public boolean hasActiveTasks() {
            for (Task task : tasks) {
                if (!task.isCompleted()) {
                    return true;
                }
            }

            return false;
        }

        public ArrayList<Task> getTasks() {
            return new ArrayList<>(tasks);
        }

        public void calculateAllPriorities() {
            for (Task task : tasks) {
                task.setPriorityScore(calculatePriority(task));
            }
        }

        public int calculatePriority(Task task) {
            if (task.isCompleted()) {
                return 0;
            }

            int score = 0;

            score += task.getImportance() * 20;

            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    task.getDeadline()
            );

            if (daysLeft <= 1) {
                score += 40;
            } else if (daysLeft <= 3) {
                score += 30;
            } else if (daysLeft <= 7) {
                score += 20;
            } else {
                score += 10;
            }

            if (task.getEstimatedHours() >= 5) {
                score += 30;
            } else if (task.getEstimatedHours() >= 3) {
                score += 20;
            } else if (task.getEstimatedHours() >= 1) {
                score += 10;
            } else {
                score += 5;
            }

            if (task.getProgress() == 0) {
                score += 30;
            } else if (task.getProgress() <= 50) {
                score += 20;
            } else if (task.getProgress() <= 80) {
                score += 10;
            }

            return score;
        }

        public ArrayList<Task> getSortedTasks() {
            calculateAllPriorities();

            ArrayList<Task> sortedTasks = new ArrayList<>(tasks);

            sortedTasks.sort(
                    Comparator.comparingInt(Task::getPriorityScore).reversed()
            );

            return sortedTasks;
        }

        public String buildPriorityText() {
            ArrayList<Task> sortedTasks = getSortedTasks();
            StringBuilder result = new StringBuilder();

            result.append("다음은 Java 프로그램이 계산한 학습 일정 우선순위입니다.\n\n");

            int number = 1;

            for (Task task : sortedTasks) {
                if (task.isCompleted()) {
                    continue;
                }

                result.append(number).append(". ")
                        .append(task.getSubject()).append(" - ")
                        .append(task.getTitle()).append("\n");

                result.append("학습 유형: ")
                        .append(task.getStudyType()).append("\n");

                result.append("마감일: ")
                        .append(task.getDeadline()).append("\n");

                result.append("중요도: ")
                        .append(task.getImportance()).append("\n");

                result.append("예상 시간: ")
                        .append(task.getEstimatedHours()).append("시간\n");

                result.append("진행률: ")
                        .append(task.getProgress()).append("%\n");

                result.append("우선순위 점수: ")
                        .append(task.getPriorityScore()).append("점\n");

                if (!task.getDetail().isBlank()) {
                    result.append("세부 내용: ")
                            .append(task.getDetail()).append("\n");
                }

                result.append("\n");
                number++;
            }

            return result.toString();
        }

        public String saveTasks(String fileName) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (Task task : tasks) {
                    writer.write(task.toFileString());
                    writer.newLine();
                }

                return "tasks.txt 파일에 저장되었습니다.";

            } catch (IOException e) {
                return "저장 중 오류가 발생했습니다.";
            }
        }

        public String loadTasks(String fileName) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                tasks.clear();

                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        tasks.add(Task.fromFileString(line));
                    }
                }

                return "tasks.txt 파일을 불러왔습니다.";

            } catch (IOException e) {
                return "저장된 tasks.txt 파일이 없습니다.";

            } catch (Exception e) {
                return "파일 형식이 잘못되었습니다.";
            }
        }
    }

    static class OpenAIService {
        private final String apiKey;
        private final HttpClient client;

        public OpenAIService() {
            apiKey = System.getenv("OPENAI_API_KEY");

            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
        }

        public String generateStudyPlan(String priorityText, double availableHours) {
            if (apiKey == null || apiKey.isBlank()) {
                return "API Key를 찾을 수 없습니다.";
            }

            String prompt =
                    "너는 학생의 학습 계획을 정리해주는 도우미이다.\n\n"
                            + "아래 우선순위는 Java 프로그램이 직접 계산한 결과이다.\n"
                            + "우선순위를 다시 계산하거나 순서를 바꾸지 말고, 주어진 순서를 따른다.\n\n"
                            + "할 일의 학습 유형과 세부 내용을 반드시 참고한다.\n"
                            + "세부 내용에 없는 약점, 오답, 시험 범위를 사실처럼 만들어내면 안 된다.\n"
                            + "세부 내용이 비어 있다면 학습 유형에 맞는 일반적인 작업 단계만 제안한다.\n\n"
                            + "오늘 공부 가능한 시간은 " + availableHours + "시간이다.\n\n"
                            + "다음 조건을 지켜 한국어로 작성한다.\n"
                            + "1. 가장 우선순위가 높은 일을 먼저 해야 하는 이유를 한 문장으로 설명한다.\n"
                            + "2. 3~5단계의 학습 계획을 작성한다.\n"
                            + "3. 각 단계에는 예상 시간을 분 단위로 포함한다.\n"
                            + "4. 전체 시간은 " + availableHours + "시간을 넘지 않게 한다.\n"
                            + "5. 완료된 일은 포함하지 않는다.\n"
                            + "6. 사용자가 입력한 세부 내용이 있으면 그 내용에 맞게 구체적으로 작성한다.\n\n"
                            + priorityText;

            String requestBody = "{"
                    + "\"model\":\"gpt-5.4-mini\","
                    + "\"input\":\"" + escapeJson(prompt) + "\","
                    + "\"max_output_tokens\":700"
                    + "}";

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/responses"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return extractText(response.body());
                }

                return "API 요청 실패\n상태 코드: "
                        + response.statusCode()
                        + "\n\n"
                        + response.body();

            } catch (Exception e) {
                return "API 연결 중 오류가 발생했습니다.\n"
                        + e.getMessage();
            }
        }

        private String escapeJson(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        private String extractText(String json) {
            Pattern pattern = Pattern.compile(
                    "\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
            );

            Matcher matcher = pattern.matcher(json);

            if (matcher.find()) {
                return unescapeJson(matcher.group(1));
            }

            return json;
        }

        private String unescapeJson(String value) {
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);

                if (c != '\\') {
                    result.append(c);
                    continue;
                }

                if (i + 1 >= value.length()) {
                    result.append(c);
                    break;
                }

                char next = value.charAt(++i);

                if (next == 'n') {
                    result.append('\n');

                } else if (next == 'r') {
                    result.append('\r');

                } else if (next == 't') {
                    result.append('\t');

                } else if (next == '"') {
                    result.append('"');

                } else if (next == '\\') {
                    result.append('\\');

                } else if (next == 'u' && i + 4 < value.length()) {
                    String hex = value.substring(i + 1, i + 5);
                    result.append((char) Integer.parseInt(hex, 16));
                    i += 4;

                } else {
                    result.append(next);
                }
            }

            return result.toString();
        }
    }
}