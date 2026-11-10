param(
  [Parameter(Mandatory = $true)]
  [string]$DocumentPath,

  [Parameter(Mandatory = $true)]
  [string]$MarkdownPath
)

$ErrorActionPreference = 'Stop'

function Get-EndRange {
  param($Document)
  $endPosition = $Document.Content.End - 1
  return $Document.Range($endPosition, $endPosition)
}

function Clear-InlineMarkdown {
  param([string]$Text)
  return ($Text -replace '\*\*', '' -replace '`', '').Trim()
}

function Add-Paragraph {
  param(
    $Document,
    [string]$Text,
    [string]$Style = 'Normal',
    [bool]$Italic = $false
  )

  $range = Get-EndRange -Document $Document
  $start = $range.Start
  $range.InsertAfter((Clear-InlineMarkdown $Text) + "`r")
  $paragraphRange = $Document.Range($start, $start + (Clear-InlineMarkdown $Text).Length)
  try { $paragraphRange.Style = $Style } catch { $paragraphRange.Style = 'Normal' }
  if ($Italic) { $paragraphRange.Font.Italic = 1 }
}

function Add-CodeBlock {
  param(
    $Document,
    [string[]]$Lines
  )

  $range = Get-EndRange -Document $Document
  $table = $Document.Tables.Add($range, 1, 1)
  try { $table.Style = 'Table Grid' } catch {}
  $table.AllowAutoFit = $true
  $cellRange = $table.Cell(1, 1).Range
  $cellRange.Text = ($Lines -join "`r")
  $cellRange.Font.Name = 'Consolas'
  $cellRange.Font.Size = 9
  $cellRange.Shading.BackgroundPatternColor = 15987699
  $table.Range.InsertParagraphAfter()
}

function Add-MarkdownTable {
  param(
    $Document,
    [object[]]$Rows
  )

  if ($Rows.Count -eq 0) { return }
  $columnCount = ($Rows | ForEach-Object { $_.Count } | Measure-Object -Maximum).Maximum
  $range = Get-EndRange -Document $Document
  $table = $Document.Tables.Add($range, $Rows.Count, $columnCount)
  try { $table.Style = 'Table Grid' } catch {}
  $table.AllowAutoFit = $true

  for ($rowIndex = 0; $rowIndex -lt $Rows.Count; $rowIndex++) {
    for ($columnIndex = 0; $columnIndex -lt $columnCount; $columnIndex++) {
      $value = if ($columnIndex -lt $Rows[$rowIndex].Count) { $Rows[$rowIndex][$columnIndex] } else { '' }
      $cellRange = $table.Cell($rowIndex + 1, $columnIndex + 1).Range
      $cellRange.Text = Clear-InlineMarkdown ([string]$value)
      $cellRange.Font.Size = 9
    }
  }

  $headerRange = $table.Rows.Item(1).Range
  $headerRange.Font.Bold = 1
  $headerRange.Shading.BackgroundPatternColor = 14277081
  $table.Rows.Item(1).HeadingFormat = -1
  try { $table.AutoFitBehavior(2) } catch {}
  $table.Range.InsertParagraphAfter()
}

function Convert-MarkdownTableRows {
  param([string[]]$Lines)

  $rows = @()
  foreach ($line in $Lines) {
    $cells = @($line.Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() })
    $isSeparator = $true
    foreach ($cell in $cells) {
      if ($cell -notmatch '^:?-{3,}:?$') { $isSeparator = $false; break }
    }
    if (-not $isSeparator) { $rows += ,$cells }
  }
  return $rows
}

if (-not (Test-Path -LiteralPath $DocumentPath)) {
  throw "SRS document not found: $DocumentPath"
}
if (-not (Test-Path -LiteralPath $MarkdownPath)) {
  throw "API markdown not found: $MarkdownPath"
}

$documentFullPath = [System.IO.Path]::GetFullPath($DocumentPath)
$markdownFullPath = [System.IO.Path]::GetFullPath($MarkdownPath)
$backupPath = Join-Path ([System.IO.Path]::GetDirectoryName($documentFullPath)) 'SRS_database_update.before_api_backup.docx'

if (-not (Test-Path -LiteralPath $backupPath)) {
  Copy-Item -LiteralPath $documentFullPath -Destination $backupPath
}

$markdownLines = Get-Content -LiteralPath $markdownFullPath -Encoding UTF8
$wordApp = New-Object -ComObject Word.Application
$wordApp.Visible = $false
$wordApp.DisplayAlerts = 0
$document = $null

try {
  $document = $wordApp.Documents.Open($documentFullPath, $false, $false)
  if ($document.Content.Text -match 'V\. THIẾT KẾ API') {
    throw 'The API chapter already exists in the SRS document.'
  }

  $breakRange = Get-EndRange -Document $document
  $breakRange.InsertBreak(7)
  Add-Paragraph -Document $document -Text 'V. THIẾT KẾ API' -Style 'Heading 2'

  $inCodeBlock = $false
  $codeLines = @()
  $index = 0

  while ($index -lt $markdownLines.Count) {
    $line = $markdownLines[$index]

    if ($line -match '^```') {
      if ($inCodeBlock) {
        Add-CodeBlock -Document $document -Lines $codeLines
        $codeLines = @()
        $inCodeBlock = $false
      } else {
        $inCodeBlock = $true
      }
      $index++
      continue
    }

    if ($inCodeBlock) {
      $codeLines += $line
      $index++
      continue
    }

    if ($line.TrimStart().StartsWith('|')) {
      $tableLines = @()
      while ($index -lt $markdownLines.Count -and $markdownLines[$index].TrimStart().StartsWith('|')) {
        $tableLines += $markdownLines[$index]
        $index++
      }
      $tableRows = Convert-MarkdownTableRows -Lines $tableLines
      Add-MarkdownTable -Document $document -Rows $tableRows
      continue
    }

    if ($line -match '^# ') {
      $index++
      continue
    }
    if ($line -match '^## (.+)$') {
      Add-Paragraph -Document $document -Text $Matches[1] -Style 'Heading 3'
      $index++
      continue
    }
    if ($line -match '^### (.+)$') {
      Add-Paragraph -Document $document -Text $Matches[1] -Style 'Heading 4'
      $index++
      continue
    }
    if ($line -match '^>\s*(.+)$') {
      Add-Paragraph -Document $document -Text $Matches[1] -Style 'Normal' -Italic $true
      $index++
      continue
    }
    if ($line -match '^-\s+(.+)$') {
      Add-Paragraph -Document $document -Text $Matches[1] -Style 'List Bullet'
      $index++
      continue
    }
    if ($line -match '^\d+\.\s+(.+)$') {
      Add-Paragraph -Document $document -Text $Matches[1] -Style 'List Number'
      $index++
      continue
    }
    if ([string]::IsNullOrWhiteSpace($line)) {
      $index++
      continue
    }

    Add-Paragraph -Document $document -Text $line -Style 'Normal'
    $index++
  }

  $document.Save()
  $paragraphCount = $document.Paragraphs.Count
  $tableCount = $document.Tables.Count
  $document.Close($true)
  $document = $null
  "Updated: $documentFullPath"
  "Backup: $backupPath"
  "Paragraphs: $paragraphCount"
  "Tables: $tableCount"
} finally {
  if ($document) {
    try { $document.Close($false) } catch {}
  }
  try { $wordApp.Quit() } catch {}
}
