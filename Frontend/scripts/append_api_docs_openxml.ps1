param(
  [Parameter(Mandatory = $true)]
  [string]$DocumentPath,

  [Parameter(Mandatory = $true)]
  [string]$MarkdownPath
)

$ErrorActionPreference = 'Stop'
$wordNamespace = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
$chapterTitle = 'V. THI' + [char]0x1EBE + 'T K' + [char]0x1EBE + ' API'

function Escape-XmlText {
  param([AllowEmptyString()][string]$Text)
  if ($null -eq $Text) { return '' }
  return [System.Security.SecurityElement]::Escape($Text)
}

function Clear-InlineMarkdown {
  param([AllowEmptyString()][string]$Text)
  return ($Text -replace '\*\*', '' -replace '`', '').Trim()
}

function New-RunXml {
  param(
    [AllowEmptyString()][string]$Text,
    [bool]$Bold = $false,
    [bool]$Italic = $false,
    [string]$FontName = '',
    [int]$FontSize = 0
  )

  $properties = ''
  if ($Bold) { $properties += '<w:b/>' }
  if ($Italic) { $properties += '<w:i/>' }
  if ($FontName) { $properties += '<w:rFonts w:ascii="' + (Escape-XmlText $FontName) + '" w:hAnsi="' + (Escape-XmlText $FontName) + '"/>' }
  if ($FontSize -gt 0) { $properties += '<w:sz w:val="' + $FontSize + '"/><w:szCs w:val="' + $FontSize + '"/>' }
  $runProperties = if ($properties) { '<w:rPr>' + $properties + '</w:rPr>' } else { '' }
  return '<w:r>' + $runProperties + '<w:t xml:space="preserve">' + (Escape-XmlText $Text) + '</w:t></w:r>'
}

function New-ParagraphXml {
  param(
    [AllowEmptyString()][string]$Text,
    [string]$Style = 'Normal',
    [bool]$Bold = $false,
    [bool]$Italic = $false
  )

  $paragraphProperties = '<w:pPr><w:pStyle w:val="' + (Escape-XmlText $Style) + '"/><w:spacing w:after="120"/></w:pPr>'
  return '<w:p>' + $paragraphProperties + (New-RunXml -Text (Clear-InlineMarkdown $Text) -Bold $Bold -Italic $Italic) + '</w:p>'
}

function New-CodeBlockXml {
  param([string[]]$Lines)

  $runs = ''
  for ($index = 0; $index -lt $Lines.Count; $index++) {
    $runs += New-RunXml -Text $Lines[$index] -FontName 'Consolas' -FontSize 18
    if ($index -lt $Lines.Count - 1) { $runs += '<w:r><w:br/></w:r>' }
  }

  return @"
<w:tbl>
  <w:tblPr>
    <w:tblStyle w:val="TableGrid"/>
    <w:tblW w:w="0" w:type="auto"/>
    <w:tblLayout w:type="autofit"/>
  </w:tblPr>
  <w:tr>
    <w:tc>
      <w:tcPr><w:shd w:fill="F2F2F2"/><w:tcMar><w:top w:w="100" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tcMar></w:tcPr>
      <w:p><w:pPr><w:spacing w:after="0"/></w:pPr>$runs</w:p>
    </w:tc>
  </w:tr>
</w:tbl>
<w:p/>
"@
}

function New-MarkdownTableXml {
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

  if ($rows.Count -eq 0) { return '' }
  $tableRowsXml = ''
  for ($rowIndex = 0; $rowIndex -lt $rows.Count; $rowIndex++) {
    $cellsXml = ''
    foreach ($cell in $rows[$rowIndex]) {
      $isHeader = $rowIndex -eq 0
      $shading = if ($isHeader) { '<w:shd w:fill="D9EAD3"/>' } else { '' }
      $cellsXml += '<w:tc><w:tcPr>' + $shading + '<w:tcW w:w="0" w:type="auto"/></w:tcPr><w:p><w:pPr><w:spacing w:after="0"/></w:pPr>' + (New-RunXml -Text (Clear-InlineMarkdown $cell) -Bold $isHeader -FontSize 16) + '</w:p></w:tc>'
    }
    $headerProperty = if ($rowIndex -eq 0) { '<w:trPr><w:tblHeader/></w:trPr>' } else { '' }
    $tableRowsXml += '<w:tr>' + $headerProperty + $cellsXml + '</w:tr>'
  }

  return '<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="0" w:type="auto"/><w:tblLayout w:type="autofit"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>' + $tableRowsXml + '</w:tbl><w:p/>'
}

if (-not (Test-Path -LiteralPath $DocumentPath)) { throw "SRS document not found: $DocumentPath" }
if (-not (Test-Path -LiteralPath $MarkdownPath)) { throw "API markdown not found: $MarkdownPath" }

$documentFullPath = [System.IO.Path]::GetFullPath($DocumentPath)
$markdownFullPath = [System.IO.Path]::GetFullPath($MarkdownPath)
$tempPath = Join-Path ([System.IO.Path]::GetDirectoryName($documentFullPath)) 'SRS_database_update.api_temp.docx'

if (Test-Path -LiteralPath $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
Copy-Item -LiteralPath $documentFullPath -Destination $tempPath

try {
  $markdownLines = Get-Content -LiteralPath $markdownFullPath -Encoding UTF8
  $parts = New-Object System.Collections.Generic.List[string]
  $parts.Add('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
  $parts.Add((New-ParagraphXml -Text $chapterTitle -Style 'Heading2'))

  $inCodeBlock = $false
  $codeLines = @()
  $index = 0

  while ($index -lt $markdownLines.Count) {
    $line = $markdownLines[$index]

    if ($line -match '^```') {
      if ($inCodeBlock) {
        $parts.Add((New-CodeBlockXml -Lines $codeLines))
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
      $parts.Add((New-MarkdownTableXml -Lines $tableLines))
      continue
    }

    if ($line -match '^# ') { $index++; continue }
    if ($line -match '^## (.+)$') { $parts.Add((New-ParagraphXml -Text $Matches[1] -Style 'Heading3')); $index++; continue }
    if ($line -match '^### (.+)$') { $parts.Add((New-ParagraphXml -Text $Matches[1] -Style 'Heading4')); $index++; continue }
    if ($line -match '^>\s*(.+)$') { $parts.Add((New-ParagraphXml -Text $Matches[1] -Style 'Normal' -Italic $true)); $index++; continue }
    if ($line -match '^-\s+(.+)$') { $parts.Add((New-ParagraphXml -Text (([char]0x2022) + ' ' + $Matches[1]) -Style 'Normal')); $index++; continue }
    if ($line -match '^(\d+)\.\s+(.+)$') { $parts.Add((New-ParagraphXml -Text ($Matches[1] + '. ' + $Matches[2]) -Style 'Normal')); $index++; continue }
    if ([string]::IsNullOrWhiteSpace($line)) { $index++; continue }

    $parts.Add((New-ParagraphXml -Text $line -Style 'Normal'))
    $index++
  }

  Add-Type -AssemblyName System.IO.Compression
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $archive = [System.IO.Compression.ZipFile]::Open($tempPath, [System.IO.Compression.ZipArchiveMode]::Update)
  try {
    $documentEntry = $archive.GetEntry('word/document.xml')
    if (-not $documentEntry) { throw 'word/document.xml is missing.' }

    $reader = New-Object System.IO.StreamReader($documentEntry.Open(), [System.Text.Encoding]::UTF8)
    $xmlText = $reader.ReadToEnd()
    $reader.Close()

    $xmlDocument = New-Object System.Xml.XmlDocument
    $xmlDocument.PreserveWhitespace = $true
    $xmlDocument.LoadXml($xmlText)
    if ($xmlDocument.DocumentElement.InnerText.Contains($chapterTitle)) { throw 'The API chapter already exists.' }

    $namespaceManager = New-Object System.Xml.XmlNamespaceManager($xmlDocument.NameTable)
    $namespaceManager.AddNamespace('w', $wordNamespace)
    $body = $xmlDocument.SelectSingleNode('//w:body', $namespaceManager)
    $sectionProperties = $body.SelectSingleNode('w:sectPr', $namespaceManager)

    $fragmentDocument = New-Object System.Xml.XmlDocument
    $fragmentDocument.PreserveWhitespace = $true
    $fragmentDocument.LoadXml('<root xmlns:w="' + $wordNamespace + '">' + ($parts -join '') + '</root>')
    foreach ($child in @($fragmentDocument.DocumentElement.ChildNodes)) {
      $importedNode = $xmlDocument.ImportNode($child, $true)
      if ($sectionProperties) { $null = $body.InsertBefore($importedNode, $sectionProperties) }
      else { $null = $body.AppendChild($importedNode) }
    }

    $documentEntry.Delete()
    $newEntry = $archive.CreateEntry('word/document.xml', [System.IO.Compression.CompressionLevel]::Optimal)
    $entryStream = $newEntry.Open()
    $writerSettings = New-Object System.Xml.XmlWriterSettings
    $writerSettings.Encoding = New-Object System.Text.UTF8Encoding($false)
    $writerSettings.Indent = $false
    $writerSettings.CloseOutput = $false
    $xmlWriter = [System.Xml.XmlWriter]::Create($entryStream, $writerSettings)
    $xmlDocument.Save($xmlWriter)
    $xmlWriter.Close()
    $entryStream.Close()
  } finally {
    $archive.Dispose()
  }

  $validationArchive = [System.IO.Compression.ZipFile]::OpenRead($tempPath)
  try {
    $validationEntry = $validationArchive.GetEntry('word/document.xml')
    $validationReader = New-Object System.IO.StreamReader($validationEntry.Open(), [System.Text.Encoding]::UTF8)
    $validationText = $validationReader.ReadToEnd()
    $validationReader.Close()
    if (-not $validationText.Contains($chapterTitle)) { throw 'API chapter validation failed.' }
  } finally {
    $validationArchive.Dispose()
  }

  Move-Item -LiteralPath $tempPath -Destination $documentFullPath -Force
  "Updated: $documentFullPath"
  "Inserted OpenXML blocks: $($parts.Count)"
  "Size: $((Get-Item -LiteralPath $documentFullPath).Length) bytes"
} catch {
  if (Test-Path -LiteralPath $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
  throw
}
