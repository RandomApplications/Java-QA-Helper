'Based On: https://www.techspot.com/articles-info/1760/images/Win10KeyFinder.txt
'Found In: https://www.techspot.com/guides/1760-find-your-windows-product-key/

Option Explicit
Dim ObjShell, Path, DigitalID, Result
Set ObjShell = CreateObject("WScript.Shell")
Path = "HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\"
DigitalID = objshell.RegRead(Path & "DigitalProductId")

Wscript.Echo "Product Name: " & ObjShell.RegRead(Path & "ProductName") & vbLf & "Product ID: " & ObjShell.RegRead(Path & "ProductID") & vbLf  & "Product Key: " & ConvertToKey(DigitalID)

Function ConvertToKey(KeyInput)
	Const KeyOffset = 52
	Dim isWin8, Maps, i, j, Current, KeyOutput, Last, KeyPart1, Insert
	
	isWin8 = (KeyInput(66) \ 6) And 1
	KeyInput(66) = (KeyInput(66) And &HF7) Or ((isWin8 And 2) * 4)
	i = 24
	Maps = "BCDFGHJKMPQRTVWXY2346789"

	Do
		Current= 0
		j = 14
		Do
			Current = Current * 256
			Current = KeyInput(j + KeyOffset) + Current
			KeyInput(j + KeyOffset) = (Current \ 24)
			Current = Current Mod 24
			j = j - 1
		Loop While j >= 0
		
		i = i - 1
		KeyOutput = Mid(Maps, Current + 1, 1) & KeyOutput
		Last = Current
	Loop While i >= 0

	If (isWin8 = 1) Then
		KeyPart1 = Mid(KeyOutput, 2, Last)
		Insert = "N"
		KeyOutput = Replace(KeyOutput, KeyPart1, KeyPart1 & Insert, 2, 1, 0)
		If Last = 0 Then KeyOutput = Insert & KeyOutput
	End If

	ConvertToKey = Mid(KeyOutput, 1, 5) & "-" & Mid(KeyOutput, 6, 5) & "-" & Mid(KeyOutput, 11, 5) & "-" & Mid(KeyOutput, 16, 5) & "-" & Mid(KeyOutput, 21, 5)
End Function